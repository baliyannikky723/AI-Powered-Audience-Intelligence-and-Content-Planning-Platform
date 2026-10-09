# Audience Memory Architecture & Mathematical Specifications

## 1. Overview
The **Audience Memory** layer transforms transient audience signals (comments, questions, recurring pain points, sentiment, cluster assignments) into a persistent, time-aware, and confidence-scored memory system.

PostgreSQL remains the authoritative **system of record** for all transactional data. Audience Memory constructs a derived, query-optimized projection in **Neo4j 5.x**.

---

## 2. Mathematical Formulations

### 2.1 Canonical Memory Confidence Formula
The confidence in an inferred audience interest is defined by:

$$\text{confidence} = \min\left(0.99, \left(1 - \exp\left(-\frac{\text{evidence\_count}}{200}\right)\right) \times \text{consistency}\right)$$

Where:
- $\text{evidence\_count}$: The total number of supporting comment observations observed within the configurable evidence window ($W = 180\text{ days}$).
- $\text{consistency}$: The semantic consistency score of supporting embeddings within the topic cluster (bounded in $[0.0, 1.0]$).
- The value is hard-capped at **$0.99$** to prevent overconfidence and reflect epistemic uncertainty.

### 2.2 Time-Aware Memory Decay Formula
Memory decays over time without reinforcing audience signals, adhering to exponential half-life decay:

$$\text{confidence}_t = \text{confidence}_0 \times \exp\left(-\ln(2) \times \frac{\Delta t}{t_{1/2}}\right)$$

Where:
- $\text{confidence}_0$: Initial base confidence derived from evidence count and consistency.
- $\Delta t$: Days elapsed since the topic was last observed in ingested audience signals ($\text{days\_since\_last\_seen}$).
- $t_{1/2}$: Half-life parameter (default **$45\text{ days}$**).

---

## 3. Memory State Machine

Topics transition dynamically through three deterministic states based on decayed confidence:

| State | Decayed Confidence Range | Description | Retention / Action |
|---|---|---|---|
| **`ACTIVE`** | $\ge 0.60$ | High audience resonance; actively reinforced. | Primary target for recommendation generation & content planning. |
| **`WEAKENING`** | $0.30 \le \text{confidence}_t < 0.60$ | Unreinforced interest undergoing half-life decay. | Tracked for churn/shift; eligible for re-engagement testing. |
| **`INACTIVE`** | $< 0.30$ | Long-dormant interest. | Retained in graph history; excluded from active recommendations. |

*Note: Inactive memories are never deleted automatically, preserving historical graph topology for research.*

---

## 4. Question Deduplication & Normalization
Audience inquiries and questions are processed through privacy filters and normalized:
1. **Redaction**: PII, email addresses, phone numbers, and handles are stripped.
2. **Normalization**: Case folding, punctuation trimming, and whitespace collapse.
3. **Deterministic Hash**: SHA-256 hash computed over normalized text to eliminate duplicate nodes while aggregating observation counts (`evidenceCount`) and tracking `firstSeenAt` / `lastSeenAt`.

---

## 5. Resilience & Fallback Guarantees
- **Resilience4j Circuit Breaker**: Wraps all Neo4j projections and graph queries.
- **Graceful Degradation**: If Neo4j is offline or experiencing latency, PostgreSQL operations proceed unblocked. `getAudienceMemoryContext` returns empty context, causing the recommendation engine to cleanly fall back to `BASELINE` mode without data loss or fabricated claims.
- **Full & Incremental Projection**: Users can trigger an idempotent graph rebuild (`POST /api/v1/memory/rebuild`) at any time to re-project PostgreSQL data into Neo4j.
