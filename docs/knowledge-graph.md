# Neo4j Knowledge Graph Schema & Projection Architecture

## 1. Graph Architecture Overview
The Knowledge Graph layer operates as a **derived relationship projection** from authoritative PostgreSQL transactional tables.

```
PostgreSQL (Authoritative)
   ↓
Memory Projection Service (Idempotent MERGE)
   ↓
Neo4j 5.x Graph Database
   ↓
Graph Retrieval Layer (KnowledgeGraphQueryService)
   ↓
Evidence-Grounded Recommendation Engine / Research Benchmarks
```

---

## 2. Graph Node Definitions

### 2.1 `AudienceNode` (`:Audience`)
Represents a single tenant audience profile.
- `id` (String, Primary Key): Deterministic UUID formatted as `aud-{userId}`.
- `userId` (String): Tenant identity.
- `createdAt` (DateTime): Timestamp of graph creation.
- `updatedAt` (DateTime): Timestamp of latest graph sync.

### 2.2 `TopicNode` (`:Topic`)
Represents an inferred audience interest topic.
- `id` (String, Primary Key): Deterministic UUID formatted as `topic-{userId}-{topicId}`.
- `userId` (String): Tenant identity.
- `topicId` (String): PostgreSQL foreign reference.
- `label` (String): Topic display name.
- `status` (String): `ACTIVE`, `WEAKENING`, or `INACTIVE`.
- `confidence` (Float): Decayed confidence score ($[0.0, 0.99]$).
- `evidenceCount` (Integer): Total observations.
- `firstSeenAt` (DateTime): Earliest recorded observation.
- `lastSeenAt` (DateTime): Most recent observation.
- `createdAt` / `updatedAt` (DateTime).

### 2.3 `QuestionNode` (`:Question`)
Represents deduplicated recurring audience questions.
- `id` (String, Primary Key): `q-{userId}-{questionHash}`.
- `userId` (String): Tenant identity.
- `questionHash` (String): SHA-256 hash of normalized text.
- `normalizedText` (String): Sanitized question string.
- `confidence` (Float): Question importance score.
- `evidenceCount` (Integer): Occurrence frequency.
- `firstSeenAt` / `lastSeenAt` (DateTime).

### 2.4 `ContentIdeaNode` (`:ContentIdea`)
Represents generated content recommendations.
- `id` (String, Primary Key): `idea-{userId}-{contentId}`.
- `userId` (String): Tenant identity.
- `title` (String): Content proposal title.
- `angle` (String): Strategic angle / hook.
- `status` (String): `SUGGESTED`, `PLANNED`, `PRODUCED`.

### 2.5 `CalendarItemNode` (`:CalendarItem`)
Represents scheduled publication slots.
- `id` (String, Primary Key): `cal-{userId}-{calendarItemId}`.
- `userId` (String): Tenant identity.
- `scheduledDate` (DateTime): Target publication timestamp.
- `platform` (String): Target platform (e.g., `YOUTUBE`, `REDDIT`, `LINKEDIN`).

---

## 3. Relationship Schema

| Relationship Type | Source Node | Target Node | Cypher Representation | Description |
|---|---|---|---|---|
| **`INTERESTED_IN`** | `:Audience` | `:Topic` | `(a:Audience)-[:INTERESTED_IN]->(t:Topic)` | Audience affinity towards a topic with confidence weight. |
| **`RELATED_TO`** | `:Topic` | `:Topic` | `(t1:Topic)-[:RELATED_TO]->(t2:Topic)` | Co-occurrence or semantic similarity between topics. |
| **`ASKS`** | `:Audience` | `:Question` | `(a:Audience)-[:ASKS]->(q:Question)` | Audience inquiries and recurring questions. |
| **`BELONGS_TO`** | `:Question` | `:Topic` | `(q:Question)-[:BELONGS_TO]->(t:Topic)` | Categorization of question into an audience interest. |
| **`GENERATES`** | `:Topic` | `:ContentIdea` | `(t:Topic)-[:GENERATES]->(c:ContentIdea)` | Content recommendation generated to address an audience interest. |
| **`SCHEDULED_ON`** | `:ContentIdea` | `:CalendarItem` | `(c:ContentIdea)-[:SCHEDULED_ON]->(cal:CalendarItem)` | Publication slot linkage. |

---

## 4. Tenant Isolation & Cypher Security Guarantees
- **No Arbitrary Cypher**: APIs never accept user-provided Cypher strings.
- **Strict Parameterization**: All Cypher executions use parameterized `bindAll(params)` via Spring Data Neo4j / `Neo4jClient`.
- **User-Scoped Constraints**: Every `MATCH`, `MERGE`, and `DELETE` includes `{userId: $userId}`.
- **Scoped Rebuild**: A rebuild operation executes `MATCH (n {userId: $userId}) DETACH DELETE n`, ensuring absolute isolation from other tenant graphs.

---

## 5. Endpoints Specification

| Method | Endpoint | Description | Role / Auth |
|---|---|---|---|
| `GET` | `/api/v1/memory/summary` | Summary of active/weakening interests, questions, and graph counts. | Authenticated User |
| `GET` | `/api/v1/memory/interests` | List audience interests filtered by optional status (`ACTIVE`, `WEAKENING`). | Authenticated User |
| `GET` | `/api/v1/memory/interests/{topicId}` | Detail of a single topic interest. | Authenticated User |
| `GET` | `/api/v1/memory/questions` | List recurring deduplicated audience questions. | Authenticated User |
| `GET` | `/api/v1/memory/topics/{topicId}/related` | Related topics based on co-occurrence in graph. | Authenticated User |
| `GET` | `/api/v1/memory/topics/{topicId}/content-ideas` | Content ideas associated with a specific topic. | Authenticated User |
| `POST` | `/api/v1/memory/rebuild` | Trigger full user-scoped graph rebuild from PostgreSQL. | Authenticated User |
