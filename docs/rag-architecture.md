# Phase 3O: Graph-Augmented RAG & Evidence Retrieval Architecture

## 1. Executive Summary & Objective

PulseGPT's **Controlled Evidence-Grounded RAG Layer** bridges structured audience signals, deep knowledge graphs, and semantic comment embeddings to synthesize verifiable, creator-aligned recommendations with traceable citations.

The objective is to retrieve the strongest relevant audience evidence *before* LLM generation, ensuring that every audience claim is backed by concrete empirical data and protected against prompt injection and hallucinated statistics.

```
USER QUERY / CONTENT REQUEST
          │
          ▼
  QUERY UNDERSTANDING
          │
  ┌───────┴───────────────────────────────────────────┐
  │                                                   │
  ▼                                                   ▼
┌───────────────────────────────┐   ┌───────────────────────────────┐
│ PostgreSQL + pgvector         │   │ Neo4j Knowledge Graph Memory  │
│ Semantic cosine similarity    │   │ Topic nodes, recurring Qs,    │
│ Comment embeddings (384d)     │   │ ACTIVE / WEAKENING interests  │
└───────────────┬───────────────┘   └───────────────┬───────────────┘
                │                                   │
                └─────────────────┬─────────────────┘
                                  │
                                  ▼
                ┌───────────────────────────────────┐
                │ Structured Audience Signals       │
                │ Topics, Trends, Questions,        │
                │ Content History, Recommendations  │
                └─────────────────┬─────────────────┘
                                  │
                                  ▼
                ┌───────────────────────────────────┐
                │ RagEvidenceFusionService          │
                │ Canonical scoring & deduplication │
                │ Source diversity & budget quota   │
                └─────────────────┬─────────────────┘
                                  │
                                  ▼
                ┌───────────────────────────────────┐
                │ Ranked Evidence Set with Citations│
                │ [E1], [E2], [E3], ...             │
                └─────────────────┬─────────────────┘
                                  │
                                  ▼
                ┌───────────────────────────────────┐
                │ LLM Generation (Gemini / FastAPI) │
                │ Strict system prompt isolation    │
                └─────────────────┬─────────────────┘
                                  │
                                  ▼
                ┌───────────────────────────────────┐
                │ RagCitationValidator              │
                │ 5-Point Verification Gate         │
                │ Prompt injection defense check    │
                └─────────────────┬─────────────────┘
                                  │
                                  ▼
                         VERIFIED RESPONSE
```

---

## 2. Multi-Source Retrieval Pipeline

The RAG layer aggregates evidence across 6 distinct modalities:

| Source Modality | Technology / Repository | Retrieval Mechanics | Scope & Security |
| :--- | :--- | :--- | :--- |
| **Comments (Semantic)** | `ProcessedCommentRepository` + pgvector | Cosine distance (`<=>`), Top-K=20, similarity threshold $\ge 0.35$ | User-scoped, PII redacted, deterministic tie-breaking |
| **Audience Memory** | `KnowledgeGraphQueryService` + Neo4j | Active/weakening topic interest nodes, confidence $\ge 0.70$ | Tenant isolated, parameterized Cypher |
| **Audience Questions** | `QuestionNode` + PostgreSQL | Question clusters, recurrence count, query hash matching | Authenticated user only |
| **Topic Clusters** | `TopicRepository` | Volume, sentiment distribution, keyword centroids | User workspace isolation |
| **Emerging Trends** | `AudienceInterest` / Graph Trend Nodes | Velocity, growth rate, cross-platform resonance | User workspace isolation |
| **Content History** | `ContentProductionAssetRepository` | Prior publications, angle novelty, format history | Creator tenant context |

---

## 3. Canonical Evidence Scoring Formula

To prevent competing or divergent ranking metrics, all candidate evidence items are evaluated using the canonical PulseGPT evidence equation:

$$\text{EvidenceScore} = 0.40 \times \text{relevance} + 0.25 \times \text{recency} + 0.20 \times \text{volume} + 0.15 \times \text{quality}$$

### Scoring Components:
1. **Relevance ($0.40$)**: Combined semantic vector cosine similarity and keyword overlap score ($[0.0, 1.0]$).
2. **Recency ($0.25$)**: Exponential time decay based on creation timestamp:
   $$\text{recency}(t) = \exp(-\lambda \cdot \Delta t_{\text{days}})$$
3. **Volume ($0.20$)**: Logarithmic saturation of supporting comments/questions:
   $$\text{volume}(v) = \min\left(1.0, \frac{\ln(1 + v)}{\ln(1 + 50)}\right)$$
4. **Quality ($0.15$)**: Spam-filtered NLP confidence and signal clarity score ($[0.0, 1.0]$).

---

## 4. Evidence Fusion, Deduplication & Budgeting

### 4.1 Deduplication
Evidence items sharing the identical `sourceType` and `sourceId` are merged, retaining the maximum relevance and confidence scores. For cross-source semantic duplicates (e.g. a raw comment verbatim repeated in a question node), similarity thresholding ($\ge 0.90$) prunes redundant representations.

### 4.2 Source Diversity & Budget Quota
To ensure no single source dominates the LLM prompt context, `maxEvidenceItems = 12` (configurable) is allocated using balanced default quotas:
- **Comments (Semantic)**: up to 5 items
- **Topics & Trends**: up to 2 items
- **Audience Questions**: up to 2 items
- **Audience Memory**: up to 2 items
- **Content History**: up to 1 item

If a category contains fewer items, the remaining quota dynamically cascades to other high-scoring sources.

---

## 5. Stable Citation & Verification System

Every evidence item accepted into the context is assigned an immutable citation token: `[E1]`, `[E2]`, ..., `[En]`.

### Verification Gate (`RagCitationValidator`):
Before returning an answer or recommendation draft to the client, the output undergoes a 5-point automated verification pass:
1. **`CITATION_VALIDITY`**: Every cited token (`[Ex]`) must resolve to a valid evidence item in the retrieved set belonging to the authenticated user.
2. **`UNSUPPORTED_CLAIMS`**: Detects fabricated quantitative claims (e.g., *"90% of your audience wants X"*, *"Your audience always prefers Y"*) without supporting statistical volume in evidence metadata.
3. **`GRAPH_CLAIM_VALIDITY`**: Assertions of *"increasing audience interest"* or *"active long-term trend"* require backing by `ACTIVE` or `GROWING` memory nodes.
4. **`PROMPT_INJECTION_DEFENSE`**: Untrusted comment strings containing adversarial directives (*"ignore previous instructions"*, *"reveal system prompt"*, *"call API"*) are neutralized as plain text.
5. **`SAFETY_PRIVACY`**: PII patterns and secret tokens are validated for complete redaction.

---

## 6. Resilience & Graceful Fallback Behavior

```
┌───────────────────────────┐
│ Neo4j Knowledge Graph     │ ──(Unavailable / Timeout)──► Degrades to PostgreSQL pgvector
│                           │                              + Structured Signals
└───────────────────────────┘                              (graphAvailable = false recorded)

┌───────────────────────────┐
│ PostgreSQL / pgvector     │ ──(Unavailable / Error)────► Returns controlled error
│                           │                              (No fabricated claims returned)
└───────────────────────────┘
```

- If Neo4j is offline or times out, the system automatically falls back to PostgreSQL pgvector and structured topics, setting `graphAvailable = false` in the retrieval metadata.
- If both semantic and structured retrieval fail, the system returns a controlled HTTP error rather than attempting ungrounded generation.

---

## 7. API Reference

### 7.1 `POST /api/v1/rag/query`
Executes end-to-end RAG retrieval, LLM generation, citation attachment, and 5-point verification.
```json
{
  "query": "What are my audience's biggest pain points with Spring Boot?",
  "generationMode": "FULL_EVIDENCE_GROUNDED",
  "maxEvidence": 12,
  "timeRangeDays": 30
}
```

### 7.2 `POST /api/v1/rag/retrieve`
Research-only endpoint returning ranked evidence, citation IDs, and scoring metadata **without calling the LLM**.

### 7.3 `POST /api/v1/rag/annotations`
Records human ground-truth relevance and correctness labels for empirical research evaluations.

### 7.4 `GET /api/v1/rag/evaluation`
Returns real-time Precision@K, Recall@K, evidence coverage, citation validity, and source diversity metrics.
