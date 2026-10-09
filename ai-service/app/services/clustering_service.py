import time
from typing import Any, Dict, List, Optional
import numpy as np
from sklearn.cluster import HDBSCAN, KMeans
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics import (
    calinski_harabasz_score,
    davies_bouldin_score,
    silhouette_score,
)
import umap

from app.schemas.clustering import (
    ClusteringCommentInput,
    ClusteringConfig,
    ClusteringMetrics,
    ClusteringRunRequest,
    ClusteringRunResponse,
    ClusterKeywordScore,
    ClusterTopicResult,
    CommentAssignmentResult,
)
from app.services.ctfidf_service import CTfidfService


class ClusteringService:
    """
    Reproducible Semantic Audience Clustering Pipeline.
    
    Pipeline architecture:
    Sentence Embeddings (384-d) -> UMAP -> HDBSCAN -> Centroids -> c-TF-IDF -> Deterministic Topic Labels
    
    Also supports BASELINE_TFIDF_KMEANS for research benchmarking.
    """

    def __init__(
        self,
        ctfidf_service: Optional[CTfidfService] = None,
        algorithm_version: str = "pulsegpt-cluster-v1",
    ):
        self.ctfidf_service = ctfidf_service or CTfidfService()
        self.algorithm_version = algorithm_version

    def cluster_comments(self, request: ClusteringRunRequest) -> ClusteringRunResponse:
        start_time = time.perf_counter()
        comments = request.comments
        config = request.config
        total_comments = len(comments)

        model_versions = {
            "algorithm": self.algorithm_version,
            "clustering_pipeline": config.algorithm,
            "umap": "0.5.12" if config.umap_enabled else "disabled",
            "hdbscan": "sklearn-hdbscan",
            "ctfidf": self.ctfidf_service.version,
        }

        # Edge case: No comments
        if total_comments == 0:
            execution_time = (time.perf_counter() - start_time) * 1000.0
            return ClusteringRunResponse(
                run_id=request.run_id,
                status="COMPLETED",
                algorithm=config.algorithm,
                algorithm_version=self.algorithm_version,
                total_comments=0,
                clustered_count=0,
                noise_count=0,
                cluster_count=0,
                topics=[],
                assignments=[],
                metrics=ClusteringMetrics(
                    cluster_count=0,
                    noise_count=0,
                    noise_ratio=0.0,
                    largest_cluster_size=0,
                    smallest_cluster_size=0,
                    average_cluster_size=0.0,
                    metric_warnings=["Input comments list is empty"],
                ),
                config=config.model_dump(),
                model_versions=model_versions,
                execution_time_ms=round(execution_time, 2),
            )

        # Extract embeddings and texts
        embeddings_matrix = np.array([c.embedding for c in comments], dtype=np.float32)
        texts = [c.text for c in comments]
        comment_ids = [c.comment_id for c in comments]

        # Dispatch based on algorithm
        if config.algorithm == "BASELINE_TFIDF_KMEANS":
            labels, probabilities, warnings = self._run_kmeans_baseline(texts, config)
            reduced_embeddings = None
        else:
            labels, probabilities, reduced_embeddings, warnings = self._run_umap_hdbscan(
                embeddings_matrix, config
            )

        # Organize comments by cluster
        cluster_assignments: List[CommentAssignmentResult] = []
        cluster_to_indices: Dict[int, List[int]] = {}
        noise_count = 0

        for idx, (cid, label) in enumerate(zip(comment_ids, labels)):
            is_noise = int(label) == -1
            cluster_id = int(label)
            prob = float(probabilities[idx]) if probabilities is not None else 1.0

            if is_noise:
                noise_count += 1
            else:
                cluster_to_indices.setdefault(cluster_id, []).append(idx)

            cluster_assignments.append(
                CommentAssignmentResult(
                    comment_id=cid,
                    cluster_id=cluster_id,
                    is_noise=is_noise,
                    membership_probability=round(prob, 4) if prob is not None else None,
                )
            )

        valid_cluster_ids = sorted([cid for cid in cluster_to_indices.keys() if cid != -1])
        cluster_count = len(valid_cluster_ids)
        clustered_count = total_comments - noise_count

        # Compute c-TF-IDF for valid clusters
        cluster_docs = {
            cid: [texts[idx] for idx in indices]
            for cid, indices in cluster_to_indices.items()
            if cid != -1
        }

        cluster_keywords = self.ctfidf_service.compute_ctfidf(
            cluster_docs=cluster_docs,
            top_n=config.top_keywords,
            ngram_range=config.ngram_range,
            min_df=config.min_df,
            max_df=config.max_df,
            max_features=config.max_vocab_size,
        )

        # Build Topic results
        topics: List[ClusterTopicResult] = []
        cluster_sizes = []

        for cid in valid_cluster_ids:
            indices = cluster_to_indices[cid]
            cluster_size = len(indices)
            cluster_sizes.append(cluster_size)

            # 1. Calculate centroid from ORIGINAL 384-d embeddings
            cluster_embeds = embeddings_matrix[indices]
            centroid_vec = np.mean(cluster_embeds, axis=0)
            norm = np.linalg.norm(centroid_vec)
            if norm > 0:
                centroid_vec = centroid_vec / norm
            centroid_list = [round(float(v), 6) for v in centroid_vec]

            # 2. Extract keywords & generate deterministic label
            keywords_scored = cluster_keywords.get(cid, [])
            raw_keywords = [k.keyword for k in keywords_scored]
            label = self._generate_topic_label(raw_keywords, fallback_id=cid)

            # 3. Metadata distributions
            sentiment_dist: Dict[str, int] = {}
            intent_dist: Dict[str, int] = {}
            lang_dist: Dict[str, int] = {}
            platform_dist: Dict[str, int] = {}

            for idx in indices:
                c = comments[idx]
                if c.sentiment:
                    sentiment_dist[c.sentiment] = sentiment_dist.get(c.sentiment, 0) + 1
                if c.intent:
                    intent_dist[c.intent] = intent_dist.get(c.intent, 0) + 1
                if c.language:
                    lang_dist[c.language] = lang_dist.get(c.language, 0) + 1
                if c.platform:
                    platform_dist[c.platform] = platform_dist.get(c.platform, 0) + 1

            # 4. Exemplar comment IDs (closest to centroid)
            exemplar_ids = self._find_exemplar_comments(
                cluster_embeds=cluster_embeds,
                centroid=centroid_vec,
                comment_ids=[comment_ids[i] for i in indices],
                top_k=min(3, cluster_size),
            )

            topics.append(
                ClusterTopicResult(
                    cluster_id=cid,
                    label=label,
                    keywords=keywords_scored,
                    raw_keywords=raw_keywords,
                    comment_count=cluster_size,
                    centroid=centroid_list,
                    sentiment_distribution=sentiment_dist,
                    intent_distribution=intent_dist,
                    language_distribution=lang_dist,
                    platform_distribution=platform_dist,
                    exemplar_comment_ids=exemplar_ids,
                )
            )

        # Sort topics descending by comment count
        topics.sort(key=lambda t: t.comment_count, reverse=True)

        # Calculate metrics
        metrics = self._calculate_metrics(
            embeddings_matrix=embeddings_matrix,
            labels=labels,
            cluster_count=cluster_count,
            noise_count=noise_count,
            total_comments=total_comments,
            cluster_sizes=cluster_sizes,
            warnings=warnings,
        )

        execution_time = (time.perf_counter() - start_time) * 1000.0

        return ClusteringRunResponse(
            run_id=request.run_id,
            status="COMPLETED",
            algorithm=config.algorithm,
            algorithm_version=self.algorithm_version,
            total_comments=total_comments,
            clustered_count=clustered_count,
            noise_count=noise_count,
            cluster_count=cluster_count,
            topics=topics,
            assignments=cluster_assignments,
            metrics=metrics,
            config=config.model_dump(),
            model_versions=model_versions,
            execution_time_ms=round(execution_time, 2),
        )

    def _run_umap_hdbscan(
        self,
        embeddings: np.ndarray,
        config: ClusteringConfig,
    ) -> tuple[np.ndarray, Optional[np.ndarray], Optional[np.ndarray], List[str]]:
        """Runs UMAP dimensionality reduction + HDBSCAN clustering."""
        n_samples = embeddings.shape[0]
        warnings: List[str] = []

        # If too few samples for clustering
        if n_samples < config.min_cluster_size:
            warnings.append(
                f"Number of samples ({n_samples}) is less than min_cluster_size ({config.min_cluster_size}). All marked as noise."
            )
            labels = np.full(n_samples, -1, dtype=int)
            probabilities = np.zeros(n_samples, dtype=float)
            return labels, probabilities, None, warnings

        # UMAP reduction
        cluster_input = embeddings
        reduced_embeddings = None

        if config.umap_enabled and n_samples >= 5:
            n_components = min(config.n_components, n_samples - 2)
            n_neighbors = min(config.n_neighbors, n_samples - 1)
            n_neighbors = max(2, n_neighbors)

            try:
                reducer = umap.UMAP(
                    n_neighbors=n_neighbors,
                    n_components=n_components,
                    min_dist=config.min_dist,
                    metric=config.metric,
                    random_state=config.random_state,
                )
                reduced_embeddings = reducer.fit_transform(embeddings)
                cluster_input = reduced_embeddings
            except Exception as e:
                warnings.append(f"UMAP reduction fallback to original embeddings due to: {str(e)}")
                cluster_input = embeddings

        # HDBSCAN clustering
        min_cluster_size = max(2, min(config.min_cluster_size, n_samples))
        min_samples = config.min_samples if config.min_samples is not None else min_cluster_size
        min_samples = min(min_samples, n_samples)

        metric = "euclidean" if reduced_embeddings is not None else "cosine"

        try:
            clusterer = HDBSCAN(
                min_cluster_size=min_cluster_size,
                min_samples=min_samples,
                metric=metric,
                cluster_selection_method=config.cluster_selection_method,
            )
            labels = clusterer.fit_predict(cluster_input)
            probabilities = getattr(clusterer, "probabilities_", None)
        except Exception as ex:
            warnings.append(f"HDBSCAN clustering error: {str(ex)}")
            labels = np.full(n_samples, -1, dtype=int)
            probabilities = np.zeros(n_samples, dtype=float)

        return labels, probabilities, reduced_embeddings, warnings

    def _run_kmeans_baseline(
        self,
        texts: List[str],
        config: ClusteringConfig,
    ) -> tuple[np.ndarray, Optional[np.ndarray], List[str]]:
        """Research baseline: TF-IDF + K-Means."""
        n_samples = len(texts)
        warnings: List[str] = ["Using BASELINE_TFIDF_KMEANS research algorithm"]

        k = config.k_clusters or max(2, min(5, n_samples // 3))
        k = min(k, n_samples)

        try:
            vectorizer = TfidfVectorizer(
                max_features=config.max_vocab_size,
                ngram_range=config.ngram_range,
                stop_words="english",
            )
            X = vectorizer.fit_transform(texts)
            kmeans = KMeans(n_clusters=k, random_state=config.random_state, n_init=10)
            labels = kmeans.fit_predict(X)
            probabilities = np.ones(n_samples, dtype=float)
        except Exception as e:
            warnings.append(f"K-Means baseline error: {str(e)}")
            labels = np.zeros(n_samples, dtype=int)
            probabilities = np.ones(n_samples, dtype=float)

        return labels, probabilities, warnings

    def _generate_topic_label(self, raw_keywords: List[str], fallback_id: int) -> str:
        """
        Generates deterministic, interpretable topic label from top 3-4 keywords.
        Example: ['battery life', 'charging speed', 'heating'] -> 'Battery Life & Charging Speed'
        """
        if not raw_keywords:
            return f"Topic #{fallback_id + 1}"

        # Take up to 3 distinctive top keywords
        selected = raw_keywords[:3]
        title_cased = [kw.strip().title() for kw in selected if kw.strip()]

        if not title_cased:
            return f"Topic #{fallback_id + 1}"

        if len(title_cased) == 1:
            return title_cased[0]
        elif len(title_cased) == 2:
            return f"{title_cased[0]} & {title_cased[1]}"
        else:
            return f"{title_cased[0]}, {title_cased[1]} & {title_cased[2]}"

    def _find_exemplar_comments(
        self,
        cluster_embeds: np.ndarray,
        centroid: np.ndarray,
        comment_ids: List[str],
        top_k: int = 3,
    ) -> List[str]:
        """Finds comments with highest cosine similarity to cluster centroid."""
        if len(comment_ids) <= top_k:
            return comment_ids

        # Dot product with normalized centroid = cosine similarity
        similarities = np.dot(cluster_embeds, centroid)
        top_indices = np.argsort(similarities)[::-1][:top_k]
        return [comment_ids[i] for i in top_indices]

    def _calculate_metrics(
        self,
        embeddings_matrix: np.ndarray,
        labels: np.ndarray,
        cluster_count: int,
        noise_count: int,
        total_comments: int,
        cluster_sizes: List[int],
        warnings: List[str],
    ) -> ClusteringMetrics:
        """Calculates clustering quality scores (Silhouette, DB, CH) where mathematically valid."""
        noise_ratio = round(noise_count / total_comments, 4) if total_comments > 0 else 0.0
        largest_size = max(cluster_sizes) if cluster_sizes else 0
        smallest_size = min(cluster_sizes) if cluster_sizes else 0
        avg_size = round(float(np.mean(cluster_sizes)), 2) if cluster_sizes else 0.0

        silhouette: Optional[float] = None
        davies_bouldin: Optional[float] = None
        calinski_harabasz: Optional[float] = None

        # Exclude noise points for cluster separation metrics if needed
        valid_mask = labels != -1
        valid_count = np.sum(valid_mask)
        valid_labels = labels[valid_mask]
        unique_valid_clusters = len(set(valid_labels))

        if unique_valid_clusters >= 2 and valid_count > unique_valid_clusters:
            try:
                valid_embeddings = embeddings_matrix[valid_mask]
                silhouette = float(silhouette_score(valid_embeddings, valid_labels, metric="cosine"))
                davies_bouldin = float(davies_bouldin_score(valid_embeddings, valid_labels))
                calinski_harabasz = float(calinski_harabasz_score(valid_embeddings, valid_labels))
            except Exception as e:
                warnings.append(f"Metrics computation skipped: {str(e)}")
        else:
            if cluster_count < 2:
                warnings.append("Silhouette/DB/CH scores require at least 2 distinct clusters.")
            elif valid_count <= unique_valid_clusters:
                warnings.append("Insufficient non-noise samples per cluster for metrics calculation.")

        return ClusteringMetrics(
            cluster_count=cluster_count,
            noise_count=noise_count,
            noise_ratio=noise_ratio,
            largest_cluster_size=largest_size,
            smallest_cluster_size=smallest_size,
            average_cluster_size=avg_size,
            silhouette_score=round(silhouette, 4) if silhouette is not None else None,
            davies_bouldin_index=round(davies_bouldin, 4) if davies_bouldin is not None else None,
            calinski_harabasz_score=round(calinski_harabasz, 4) if calinski_harabasz is not None else None,
            metric_warnings=warnings,
        )
