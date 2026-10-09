from typing import Any, Dict, List, Optional, Tuple
from pydantic import BaseModel, Field


class ClusteringCommentInput(BaseModel):
    comment_id: str
    text: str
    embedding: List[float]
    language: Optional[str] = None
    sentiment: Optional[str] = None
    intent: Optional[str] = None
    platform: Optional[str] = None
    published_at: Optional[str] = None


class ClusteringConfig(BaseModel):
    algorithm: str = Field(default="SEMANTIC_UMAP_HDBSCAN_CTFIDF", description="Clustering algorithm")
    umap_enabled: bool = Field(default=True, description="Enable UMAP dimensionality reduction")
    random_state: int = Field(default=42, description="Deterministic random seed")
    n_neighbors: int = Field(default=15, description="UMAP number of neighbors")
    n_components: int = Field(default=5, description="UMAP target dimensionality")
    min_dist: float = Field(default=0.0, description="UMAP minimum distance")
    metric: str = Field(default="cosine", description="Distance metric")
    min_cluster_size: int = Field(default=3, description="HDBSCAN minimum cluster size")
    min_samples: Optional[int] = Field(default=None, description="HDBSCAN min_samples (defaults to min_cluster_size)")
    cluster_selection_method: str = Field(default="eom", description="HDBSCAN cluster selection method: eom or leaf")
    top_keywords: int = Field(default=10, description="Top c-TF-IDF keywords per topic")
    ngram_range: Tuple[int, int] = Field(default=(1, 2), description="c-TF-IDF n-gram range")
    min_df: int = Field(default=1, description="c-TF-IDF minimum document frequency")
    max_df: float = Field(default=1.0, description="c-TF-IDF maximum document frequency")
    max_vocab_size: int = Field(default=5000, description="Maximum vocabulary size for c-TF-IDF")
    # For baseline comparison:
    k_clusters: Optional[int] = Field(default=None, description="Target clusters for K-Means baseline")


class ClusterKeywordScore(BaseModel):
    keyword: str
    score: float


class ClusterTopicResult(BaseModel):
    cluster_id: int
    label: str
    keywords: List[ClusterKeywordScore]
    raw_keywords: List[str]
    comment_count: int
    centroid: List[float]
    sentiment_distribution: Dict[str, int] = Field(default_factory=dict)
    intent_distribution: Dict[str, int] = Field(default_factory=dict)
    language_distribution: Dict[str, int] = Field(default_factory=dict)
    platform_distribution: Dict[str, int] = Field(default_factory=dict)
    exemplar_comment_ids: List[str] = Field(default_factory=list)


class CommentAssignmentResult(BaseModel):
    comment_id: str
    cluster_id: int
    is_noise: bool
    membership_probability: Optional[float] = None


class ClusteringMetrics(BaseModel):
    cluster_count: int
    noise_count: int
    noise_ratio: float
    largest_cluster_size: int
    smallest_cluster_size: int
    average_cluster_size: float
    silhouette_score: Optional[float] = None
    davies_bouldin_index: Optional[float] = None
    calinski_harabasz_score: Optional[float] = None
    metric_warnings: List[str] = Field(default_factory=list)


class ClusteringRunRequest(BaseModel):
    run_id: str
    comments: List[ClusteringCommentInput]
    config: ClusteringConfig = Field(default_factory=ClusteringConfig)


class ClusteringRunResponse(BaseModel):
    run_id: str
    status: str
    algorithm: str
    algorithm_version: str
    total_comments: int
    clustered_count: int
    noise_count: int
    cluster_count: int
    topics: List[ClusterTopicResult]
    assignments: List[CommentAssignmentResult]
    metrics: ClusteringMetrics
    config: Dict[str, Any]
    model_versions: Dict[str, str]
    execution_time_ms: float
