# Phase 3H: Audience Clustering & Topic Modeling Architecture

## 1. Overview & Pipeline Architecture

The **PulseGPT Audience Intelligence Platform** processes large volumes of unstructured audience feedback (e.g. YouTube comments, social posts) and transforms them into interpretable, high-signal topic clusters to drive content strategy.

```
Processed Comments (with 384-d Dense Embeddings)
                        ↓
UMAP Dimensionality Reduction (Cosine Metric, Fixed Random Seed)
                        ↓
HDBSCAN Density-Based Clustering (Noise / Outlier Preservation)
                        ↓
Original 384-d Embeddings Centroid Extraction (L2-Normalized)
                        ↓
Class-based TF-IDF (c-TF-IDF) Multi-lingual Keyword Extraction
                        ↓
Deterministic Human-Readable Topic Label Generation
                        ↓
Clustering Quality Evaluation Metrics (Silhouette, DB, CH)
                        ↓
Centroid-Based Stable Topic Identity Matching & Persistence
                        ↓
User-Scoped REST APIs & Longitudinal Audit Logging
```

---

## 2. Why Sentence Embeddings (Dense Vector Space)?

- **Semantic Understanding**: Unlike traditional bag-of-words or sparse TF-IDF vectors, dense embeddings (`all-MiniLM-L6-v2`, 384 dimensions) capture synonyms, paraphrases, contextual tone, and cross-lingual intent.
- **Efficiency & Reuse**: Embeddings generated during the Phase 3G NLP pipeline are stored directly in PostgreSQL and reused during clustering runs, avoiding redundant inference cost.

---

## 3. Why UMAP (Uniform Manifold Approximation and Projection)?

- **High-Dimensional Geometry Preservation**: High-dimensional vector spaces suffer from the *curse of dimensionality* (distance concentration). UMAP preserves both local and global manifold structure.
- **Cosine Metric Alignment**: Sentence transformer cosine distances are natively preserved using `metric='cosine'`.
- **Target Dimensionality**: Embeddings are reduced to 5 dimensions (preserving rich geometric topology) rather than 2D, which is reserved for visualization.
- **Reproducibility**: All UMAP runs utilize an explicit, deterministic `random_state=42`.

---

## 4. Why HDBSCAN (Hierarchical Density-Based Clustering)?

- **No Arbitrary Cluster Count ($K$)**: Traditional K-Means forces arbitrary spherical partitioning and requires pre-specifying $K$. HDBSCAN automatically identifies the natural number and shapes of clusters based on density.
- **Noise & Outlier Preservation**: In audience feedback, noisy or off-topic comments (e.g. spam, random chat) should **never** be forcefully assigned to topics. HDBSCAN preserves unclustered points as outliers ($cluster\_id = -1, is\_noise = True$).
- **Membership Probabilities**: Exposes soft cluster membership probabilities ($p \in [0, 1]$) for ranking comment relevance.

---

## 5. Why Class-Based TF-IDF (c-TF-IDF)?

Standard TF-IDF measures word importance across individual documents. However, a topic is a collection of documents. **c-TF-IDF** treats all documents within a cluster as a single composite class document:

$$W_{t, c} = \text{tf}_{t, c} \times \log\left(1 + \frac{A}{\text{tf}_t}\right)$$

Where:
- $\text{tf}_{t, c}$: Frequency of term $t$ in cluster $c$.
- $\text{tf}_t$: Global frequency of term $t$ across all clusters.
- $A$: Average number of words per cluster class.

### Key Benefits:
- Extracts terms that uniquely distinguish one audience topic from other topics.
- Multi-lingual tokenization handles mixed English and romanized Hinglish comments.
- Custom stop-word filtering prevents generic video terminology from dominating keywords.

---

## 6. Stable Topic Identity Across Time

Clustering algorithms produce arbitrary cluster indices on each run ($cluster\_0$ today might be completely different tomorrow). To enable longitudinal tracking:

1. For each discovered cluster, its **Centroid** is computed in the original 384-dimensional embedding space:
   $$C = \frac{1}{|S|} \sum_{v \in S} v, \quad \hat{C} = \frac{C}{\|C\|_2}$$
2. The centroid is compared against existing active topics for the user using **Cosine Similarity**:
   $$\text{sim}(\hat{C}_{new}, \hat{C}_{existing}) \ge \tau \quad (\tau = 0.82)$$
3. If similarity exceeds the threshold:
   - Topic identity is **reused** (stable topic ID).
   - Topic comment count, keyword scores, and distributions (sentiment, intent, language, platform) are merged.
   - Centroid is updated via exponential moving average.
4. If no match is found, a **new Topic identity** is registered.

---

## 7. Research Reproducibility & Benchmarking

Every clustering execution creates an immutable `ClusteringRun` record containing:
- Full dataset window (`timeWindowStart`, `timeWindowEnd`)
- Algorithm configuration (`algorithm`, `algorithmVersion`, `config` JSON)
- Model metadata (`embeddingModel`, `embeddingDimension`, `modelVersions`)
- Quantitative clustering evaluation metrics (`silhouetteScore`, `daviesBouldinIndex`, `calinskiHarabaszScore`, `noiseRatio`, cluster size statistics)

### Research Baseline Support:
- `SEMANTIC_UMAP_HDBSCAN_CTFIDF` (Primary Production Pipeline)
- `BASELINE_TFIDF_KMEANS` (Research Comparative Baseline)

---

## 8. Multi-Tenant Security & API Endpoints

All database queries, topic associations, and clustering triggers are strictly user-scoped via Spring Security JWT context:
- `POST /api/v1/topics/cluster`: Trigger clustering run with custom parameters.
- `GET /api/v1/topics`: Query paginated topics with search, active status, and sorting.
- `GET /api/v1/topics/{id}`: Detailed topic intelligence (distributions, keywords, scores).
- `GET /api/v1/topics/{id}/comments`: Paginated comments assigned to a topic.
- `GET /api/v1/clustering/runs`: Audit history of clustering runs.
- `GET /api/v1/clustering/runs/{id}`: Full run metadata and evaluation metrics.
