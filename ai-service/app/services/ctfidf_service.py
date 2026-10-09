import re
from typing import Dict, List, Tuple
import numpy as np
from sklearn.feature_extraction.text import CountVectorizer

from app.schemas.clustering import ClusterKeywordScore


class CTfidfService:
    """
    Class-based TF-IDF (c-TF-IDF) representation service.
    
    Treats all comments belonging to a cluster as a single composite document,
    then computes term frequency per cluster weighted by the inverse total frequency
    across all clusters:
        W_{t, c} = tf_{t, c} * log(1 + (A / tf_t))
    where A is the average number of words per cluster class.
    """

    def __init__(self, version: str = "1.0.0"):
        self.version = version

    def compute_ctfidf(
        self,
        cluster_docs: Dict[int, List[str]],
        top_n: int = 10,
        ngram_range: Tuple[int, int] = (1, 2),
        min_df: int = 1,
        max_df: float = 1.0,
        max_features: int = 5000,
    ) -> Dict[int, List[ClusterKeywordScore]]:
        """
        Computes top keywords and scores for each cluster using c-TF-IDF.
        
        Args:
            cluster_docs: Mapping of cluster_id -> list of raw/cleaned comment texts
            top_n: Number of keywords to return per cluster
            ngram_range: N-gram range for tokenization
            min_df: Minimum document frequency
            max_df: Maximum document frequency
            max_features: Maximum vocabulary size
            
        Returns:
            Mapping of cluster_id -> list of ClusterKeywordScore
        """
        if not cluster_docs:
            return {}

        cluster_ids = sorted(list(cluster_docs.keys()))
        # Combine all texts per cluster into a single document
        composite_docs = []
        for cid in cluster_ids:
            texts = cluster_docs[cid]
            cleaned_texts = [self._preprocess_text(t) for t in texts if t and t.strip()]
            combined = " ".join(cleaned_texts)
            composite_docs.append(combined if combined.strip() else "topic")

        # Custom multi-lingual token pattern that handles English, Hindi/Devanagari, and alphanumeric terms
        token_pattern = r"(?u)\b[\w\u0900-\u097F]{2,}\b"

        # Multi-lingual stop words (English common words)
        stopwords = {
            "the", "and", "is", "in", "it", "to", "for", "of", "on", "that", "this", "with",
            "you", "i", "at", "by", "from", "as", "be", "was", "are", "have", "has", "had",
            "not", "but", "what", "all", "were", "when", "we", "there", "can", "an", "your",
            "which", "their", "if", "do", "will", "so", "up", "out", "or", "about", "who",
            "my", "get", "just", "like", "they", "me", "very", "also", "would", "how", "been",
            "hai", "bhi", "ki", "ka", "ke", "ko", "se", "ho", "kya", "ye", "yeh", "woh", "kar",
            "kr", "raha", "rahe", "hu", "mera", "meri", "karo", "please", "video", "bro", "sir"
        }

        try:
            vectorizer = CountVectorizer(
                token_pattern=token_pattern,
                ngram_range=ngram_range,
                min_df=min_df,
                max_df=max_df,
                max_features=max_features,
                stop_words=list(stopwords),
            )
            # Count matrix X shape: (num_clusters, vocab_size)
            X = vectorizer.fit_transform(composite_docs).toarray()
            vocab = np.array(vectorizer.get_feature_names_out())
        except Exception:
            # Fallback with relaxed constraints if vocabulary is too small or uniform
            try:
                vectorizer = CountVectorizer(
                    ngram_range=(1, 1),
                    min_df=1,
                    max_features=max_features,
                )
                X = vectorizer.fit_transform(composite_docs).toarray()
                vocab = np.array(vectorizer.get_feature_names_out())
            except Exception:
                # If still empty (e.g. empty strings), return dummy keywords
                return {cid: [ClusterKeywordScore(keyword=f"topic_{cid}", score=1.0)] for cid in cluster_ids}

        if len(vocab) == 0:
            return {cid: [ClusterKeywordScore(keyword=f"topic_{cid}", score=1.0)] for cid in cluster_ids}

        # Compute term frequency per cluster: tf_{t, c}
        # Compute global term frequency: tf_t = sum_c(tf_{t, c})
        total_words_per_class = X.sum(axis=1, keepdims=True) # shape: (num_clusters, 1)
        total_words_per_class = np.maximum(total_words_per_class, 1)
        
        # Normalized tf within cluster
        tf = X / total_words_per_class

        # Global frequency across all classes
        global_tf = X.sum(axis=0) # shape: (vocab_size,)
        global_tf = np.maximum(global_tf, 1)

        # Average words per class
        A = np.mean(total_words_per_class)
        if A <= 0:
            A = 1.0

        # c-TF-IDF formula: W_{t, c} = tf_{t, c} * log(1 + (A / global_tf))
        idf = np.log(1.0 + (A / global_tf)) # shape: (vocab_size,)
        c_tfidf = tf * idf # shape: (num_clusters, vocab_size)

        result: Dict[int, List[ClusterKeywordScore]] = {}
        for idx, cid in enumerate(cluster_ids):
            scores = c_tfidf[idx]
            # Get top N indices sorted descending
            top_indices = np.argsort(scores)[::-1][:top_n]
            
            keywords = []
            for term_idx in top_indices:
                score_val = float(scores[term_idx])
                term_name = str(vocab[term_idx])
                if score_val > 0.0:
                    keywords.append(ClusterKeywordScore(keyword=term_name, score=round(score_val, 4)))
            
            # If all scores were 0 (e.g. single word), take highest term frequency
            if not keywords and len(vocab) > 0:
                highest_tf_idx = np.argsort(X[idx])[::-1][:top_n]
                for h_idx in highest_tf_idx:
                    keywords.append(ClusterKeywordScore(keyword=str(vocab[h_idx]), score=1.0))

            result[cid] = keywords

        return result

    def _preprocess_text(self, text: str) -> str:
        """Light preprocessing to preserve unicode and clean punctuation."""
        if not text:
            return ""
        # Remove URLs and extra symbols while keeping words & unicode
        text = re.sub(r"http\S+|www\.\S+", "", text)
        text = re.sub(r"[^\w\s\u0900-\u097F]", " ", text)
        return " ".join(text.lower().split())
