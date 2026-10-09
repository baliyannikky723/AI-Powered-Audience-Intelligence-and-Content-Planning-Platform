package com.pulsegpt.graph.repository;

import com.pulsegpt.memory.AudienceInterestStatus;
import com.pulsegpt.memory.dto.AudienceInterestResponse;
import com.pulsegpt.memory.dto.AudienceQuestionResponse;
import com.pulsegpt.memory.dto.RelatedTopicResponse;
import com.pulsegpt.memory.dto.TopicContentIdeaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class Neo4jKnowledgeGraphRepository {

    private final Neo4jClient neo4jClient;

    public void mergeAudience(String userId, Instant now) {
        String query = """
                MERGE (a:Audience {id: $audienceId, userId: $userId})
                ON CREATE SET a.createdAt = $now, a.updatedAt = $now
                ON MATCH SET a.updatedAt = $now
                """;
        neo4jClient.query(query)
                .bindAll(Map.of(
                        "audienceId", "audience:" + userId,
                        "userId", userId,
                        "now", now.toString()
                ))
                .run();
    }

    public void mergeTopic(String userId, String topicId, String label, String status,
                           double confidence, Instant firstSeenAt, Instant lastSeenAt,
                           int evidenceCount, Instant now) {
        String query = """
                MERGE (a:Audience {id: $audienceId, userId: $userId})
                ON CREATE SET a.createdAt = $now, a.updatedAt = $now
                MERGE (t:Topic {id: $topicId, userId: $userId})
                ON CREATE SET t.topicId = $rawTopicId, t.label = $label, t.status = $status,
                              t.confidence = $confidence, t.firstSeenAt = $firstSeenAt,
                              t.lastSeenAt = $lastSeenAt, t.evidenceCount = $evidenceCount,
                              t.createdAt = $now, t.updatedAt = $now
                ON MATCH SET t.label = $label, t.status = $status, t.confidence = $confidence,
                             t.lastSeenAt = $lastSeenAt, t.evidenceCount = $evidenceCount,
                             t.updatedAt = $now
                MERGE (a)-[r:INTERESTED_IN]->(t)
                SET r.confidence = $confidence, r.evidenceCount = $evidenceCount,
                    r.lastSeenAt = $lastSeenAt, r.status = $status
                """;
        Map<String, Object> params = new HashMap<>();
        params.put("audienceId", "audience:" + userId);
        params.put("topicId", "topic:" + topicId);
        params.put("rawTopicId", topicId);
        params.put("userId", userId);
        params.put("label", label != null ? label : "");
        params.put("status", status != null ? status : "ACTIVE");
        params.put("confidence", confidence);
        params.put("firstSeenAt", firstSeenAt != null ? firstSeenAt.toString() : now.toString());
        params.put("lastSeenAt", lastSeenAt != null ? lastSeenAt.toString() : now.toString());
        params.put("evidenceCount", evidenceCount);
        params.put("now", now.toString());

        neo4jClient.query(query)
                .bindAll(params)
                .run();
    }

    public void mergeQuestion(String userId, String questionHash, String normalizedText,
                              String topicId, double confidence, Instant firstSeenAt,
                              Instant lastSeenAt, int evidenceCount, Instant now) {
        String query = """
                MERGE (a:Audience {id: $audienceId, userId: $userId})
                ON CREATE SET a.createdAt = $now, a.updatedAt = $now
                MERGE (q:Question {id: $questionId, userId: $userId})
                ON CREATE SET q.questionHash = $questionHash, q.normalizedText = $normalizedText,
                              q.confidence = $confidence, q.firstSeenAt = $firstSeenAt,
                              q.lastSeenAt = $lastSeenAt, q.evidenceCount = $evidenceCount,
                              q.createdAt = $now, q.updatedAt = $now
                ON MATCH SET q.normalizedText = $normalizedText, q.confidence = $confidence,
                             q.lastSeenAt = $lastSeenAt, q.evidenceCount = $evidenceCount,
                             q.updatedAt = $now
                MERGE (a)-[r:ASKS]->(q)
                SET r.confidence = $confidence, r.evidenceCount = $evidenceCount, r.lastSeenAt = $lastSeenAt
                """;

        neo4jClient.query(query)
                .bindAll(Map.of(
                        "audienceId", "audience:" + userId,
                        "questionId", "question:" + questionHash,
                        "questionHash", questionHash,
                        "normalizedText", normalizedText != null ? normalizedText : "",
                        "userId", userId,
                        "confidence", confidence,
                        "firstSeenAt", firstSeenAt != null ? firstSeenAt.toString() : now.toString(),
                        "lastSeenAt", lastSeenAt != null ? lastSeenAt.toString() : now.toString(),
                        "evidenceCount", evidenceCount,
                        "now", now.toString()
                ))
                .run();

        if (topicId != null && !topicId.isBlank()) {
            String belongsQuery = """
                    MATCH (q:Question {id: $questionId, userId: $userId})
                    MATCH (t:Topic {id: $topicId, userId: $userId})
                    MERGE (q)-[:BELONGS_TO]->(t)
                    """;
            neo4jClient.query(belongsQuery)
                    .bindAll(Map.of(
                            "questionId", "question:" + questionHash,
                            "topicId", "topic:" + topicId,
                            "userId", userId
                    ))
                    .run();
        }
    }

    public void mergeTopicRelation(String userId, String topicId1, String topicId2,
                                   double weight, int cooccurrenceCount) {
        String query = """
                MATCH (t1:Topic {id: $t1Id, userId: $userId})
                MATCH (t2:Topic {id: $t2Id, userId: $userId})
                WHERE t1.id <> t2.id
                MERGE (t1)-[r:RELATED_TO]-(t2)
                SET r.weight = $weight, r.cooccurrenceCount = $cooccurrenceCount
                """;
        neo4jClient.query(query)
                .bindAll(Map.of(
                        "t1Id", "topic:" + topicId1,
                        "t2Id", "topic:" + topicId2,
                        "userId", userId,
                        "weight", weight,
                        "cooccurrenceCount", cooccurrenceCount
                ))
                .run();
    }

    public void mergeContentIdea(String userId, String recommendationId, String topicId,
                                 String title, String angle, String status, Instant now) {
        String query = """
                MERGE (c:ContentIdea {id: $recId, userId: $userId})
                ON CREATE SET c.title = $title, c.angle = $angle, c.status = $status,
                              c.createdAt = $now, c.updatedAt = $now
                ON MATCH SET c.title = $title, c.angle = $angle, c.status = $status,
                             c.updatedAt = $now
                """;
        neo4jClient.query(query)
                .bindAll(Map.of(
                        "recId", "rec:" + recommendationId,
                        "userId", userId,
                        "title", title != null ? title : "",
                        "angle", angle != null ? angle : "",
                        "status", status != null ? status : "DRAFT",
                        "now", now.toString()
                ))
                .run();

        if (topicId != null && !topicId.isBlank()) {
            String linkQuery = """
                    MATCH (t:Topic {id: $topicId, userId: $userId})
                    MATCH (c:ContentIdea {id: $recId, userId: $userId})
                    MERGE (t)-[:GENERATES]->(c)
                    """;
            neo4jClient.query(linkQuery)
                    .bindAll(Map.of(
                            "topicId", "topic:" + topicId,
                            "recId", "rec:" + recommendationId,
                            "userId", userId
                    ))
                    .run();
        }
    }

    public void mergeCalendarItem(String userId, String calendarItemId, String recommendationId,
                                  String title, Instant scheduledAt, String platform, String status) {
        String query = """
                MERGE (cal:CalendarItem {id: $calId, userId: $userId})
                ON CREATE SET cal.title = $title, cal.scheduledAt = $scheduledAt,
                              cal.platform = $platform, cal.status = $status
                ON MATCH SET cal.title = $title, cal.scheduledAt = $scheduledAt,
                             cal.platform = $platform, cal.status = $status
                """;
        neo4jClient.query(query)
                .bindAll(Map.of(
                        "calId", "cal:" + calendarItemId,
                        "userId", userId,
                        "title", title != null ? title : "",
                        "scheduledAt", scheduledAt != null ? scheduledAt.toString() : "",
                        "platform", platform != null ? platform : "",
                        "status", status != null ? status : "SCHEDULED"
                ))
                .run();

        if (recommendationId != null && !recommendationId.isBlank()) {
            String linkQuery = """
                    MATCH (c:ContentIdea {id: $recId, userId: $userId})
                    MATCH (cal:CalendarItem {id: $calId, userId: $userId})
                    MERGE (c)-[:SCHEDULED_ON]->(cal)
                    """;
            neo4jClient.query(linkQuery)
                    .bindAll(Map.of(
                            "recId", "rec:" + recommendationId,
                            "calId", "cal:" + calendarItemId,
                            "userId", userId
                    ))
                    .run();
        }
    }

    public void clearUserGraph(String userId) {
        String query = """
                MATCH (n {userId: $userId})
                DETACH DELETE n
                """;
        neo4jClient.query(query)
                .bindAll(Map.of("userId", userId))
                .run();
    }

    public List<AudienceInterestResponse> findInterestsByStatus(String userId, String status) {
        String query = """
                MATCH (a:Audience {userId: $userId})-[r:INTERESTED_IN]->(t:Topic {userId: $userId})
                WHERE ($status IS NULL OR r.status = $status)
                RETURN t.topicId AS topicId, t.label AS label, r.confidence AS confidence,
                       r.evidenceCount AS evidenceCount, r.lastSeenAt AS lastSeenAt,
                       r.status AS status
                ORDER BY r.confidence DESC
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("status", status);

        return new ArrayList<>(neo4jClient.query(query)
                .bindAll(params)
                .fetchAs(AudienceInterestResponse.class)
                .mappedBy((typeSystem, record) -> {
                    String rawTopicId = record.get("topicId").asString();
                    String label = record.get("label").asString();
                    Double conf = record.get("confidence").asDouble();
                    int evCount = record.get("evidenceCount").asInt();
                    String lastSeenStr = record.get("lastSeenAt").asString();
                    String statStr = record.get("status").asString();

                    Instant lastSeen = parseInstantSafe(lastSeenStr);
                    AudienceInterestStatus st = parseStatusSafe(statStr);
                    UUID tid = parseUuidSafe(rawTopicId);

                    return AudienceInterestResponse.builder()
                            .id(tid)
                            .topicId(tid)
                            .topicName(label)
                            .confidence(Math.round(conf * 100.0) / 100.0)
                            .evidenceCount(evCount)
                            .lastSeenAt(lastSeen)
                            .status(st)
                            .halfLifeDays(45)
                            .trend(conf >= 0.7 ? "GROWING" : (conf >= 0.4 ? "STABLE" : "WEAKENING"))
                            .evidenceIds(List.of("topic:" + rawTopicId))
                            .build();
                })
                .all());
    }

    public List<AudienceQuestionResponse> findRecurringQuestions(String userId, int limit) {
        String query = """
                MATCH (a:Audience {userId: $userId})-[r:ASKS]->(q:Question {userId: $userId})
                OPTIONAL MATCH (q)-[:BELONGS_TO]->(t:Topic {userId: $userId})
                RETURN q.id AS id, q.questionHash AS questionHash, q.normalizedText AS text,
                       q.confidence AS confidence, q.evidenceCount AS evidenceCount,
                       q.firstSeenAt AS firstSeenAt, q.lastSeenAt AS lastSeenAt,
                       t.topicId AS topicId, t.label AS topicName
                ORDER BY q.evidenceCount DESC, q.confidence DESC
                LIMIT $limit
                """;

        return new ArrayList<>(neo4jClient.query(query)
                .bindAll(Map.of("userId", userId, "limit", limit))
                .fetchAs(AudienceQuestionResponse.class)
                .mappedBy((typeSystem, record) -> {
                    String qid = record.get("id").asString();
                    String qHash = record.get("questionHash").asString();
                    String text = record.get("text").asString();
                    Double conf = record.get("confidence").asDouble();
                    int evCount = record.get("evidenceCount").asInt();
                    String firstSeenStr = record.get("firstSeenAt").asString();
                    String lastSeenStr = record.get("lastSeenAt").asString();
                    String rawTopicId = record.get("topicId").isNull() ? null : record.get("topicId").asString();
                    String topicName = record.get("topicName").isNull() ? null : record.get("topicName").asString();

                    return AudienceQuestionResponse.builder()
                            .id(qid)
                            .questionHash(qHash)
                            .normalizedText(text)
                            .confidence(Math.round(conf * 100.0) / 100.0)
                            .evidenceCount(evCount)
                            .firstSeenAt(parseInstantSafe(firstSeenStr))
                            .lastSeenAt(parseInstantSafe(lastSeenStr))
                            .topicId(parseUuidSafe(rawTopicId))
                            .topicName(topicName)
                            .build();
                })
                .all());
    }

    public List<RelatedTopicResponse> findRelatedTopics(String userId, String topicId, int limit) {
        String query = """
                MATCH (t1:Topic {id: $t1Id, userId: $userId})-[r:RELATED_TO]-(t2:Topic {userId: $userId})
                RETURN t1.topicId AS sourceId, t2.topicId AS targetId, t2.label AS targetName,
                       r.weight AS weight, r.cooccurrenceCount AS count
                ORDER BY r.weight DESC
                LIMIT $limit
                """;

        return new ArrayList<>(neo4jClient.query(query)
                .bindAll(Map.of("t1Id", "topic:" + topicId, "userId", userId, "limit", limit))
                .fetchAs(RelatedTopicResponse.class)
                .mappedBy((typeSystem, record) -> {
                    String sId = record.get("sourceId").asString();
                    String tId = record.get("targetId").asString();
                    String tName = record.get("targetName").asString();
                    Double weight = record.get("weight").asDouble();
                    int count = record.get("count").asInt();

                    return RelatedTopicResponse.builder()
                            .sourceTopicId(parseUuidSafe(sId))
                            .targetTopicId(parseUuidSafe(tId))
                            .targetTopicName(tName)
                            .weight(Math.round(weight * 100.0) / 100.0)
                            .cooccurrenceCount(count)
                            .build();
                })
                .all());
    }

    public List<TopicContentIdeaResponse> findTopicContentIdeas(String userId, String topicId) {
        String query = """
                MATCH (t:Topic {id: $topicId, userId: $userId})-[:GENERATES]->(c:ContentIdea {userId: $userId})
                OPTIONAL MATCH (c)-[:SCHEDULED_ON]->(cal:CalendarItem {userId: $userId})
                RETURN c.id AS id, t.topicId AS topicId, c.title AS title, c.angle AS angle,
                       c.status AS status, c.createdAt AS createdAt, cal.scheduledAt AS scheduledAt,
                       cal.platform AS platform
                ORDER BY c.createdAt DESC
                """;

        return new ArrayList<>(neo4jClient.query(query)
                .bindAll(Map.of("topicId", "topic:" + topicId, "userId", userId))
                .fetchAs(TopicContentIdeaResponse.class)
                .mappedBy((typeSystem, record) -> {
                    String recId = record.get("id").asString().replace("rec:", "");
                    String rawTopicId = record.get("topicId").asString();
                    String title = record.get("title").asString();
                    String angle = record.get("angle").asString();
                    String status = record.get("status").asString();
                    String createdStr = record.get("createdAt").asString();
                    String schedStr = record.get("scheduledAt").isNull() ? null : record.get("scheduledAt").asString();
                    String platform = record.get("platform").isNull() ? null : record.get("platform").asString();

                    return TopicContentIdeaResponse.builder()
                            .id(parseUuidSafe(recId))
                            .topicId(parseUuidSafe(rawTopicId))
                            .title(title)
                            .angle(angle)
                            .status(status)
                            .createdAt(parseInstantSafe(createdStr))
                            .scheduledAt(schedStr != null && !schedStr.isBlank() ? parseInstantSafe(schedStr) : null)
                            .platform(platform)
                            .build();
                })
                .all());
    }

    public Map<String, Integer> countUserGraphElements(String userId) {
        String query = """
                MATCH (n {userId: $userId})
                OPTIONAL MATCH (n)-[r]->(m {userId: $userId})
                RETURN count(DISTINCT n) AS nodeCount, count(DISTINCT r) AS relCount
                """;

        return neo4jClient.query(query)
                .bindAll(Map.of("userId", userId))
                .fetchAs(Map.class)
                .mappedBy((typeSystem, record) -> Map.of(
                        "nodeCount", record.get("nodeCount").asInt(),
                        "relCount", record.get("relCount").asInt()
                ))
                .one()
                .orElse(Map.of("nodeCount", 0, "relCount", 0));
    }

    private Instant parseInstantSafe(String text) {
        if (text == null || text.isBlank()) return Instant.now();
        try {
            return Instant.parse(text);
        } catch (Exception e) {
            return Instant.now();
        }
    }

    private AudienceInterestStatus parseStatusSafe(String text) {
        if (text == null) return AudienceInterestStatus.ACTIVE;
        try {
            return AudienceInterestStatus.valueOf(text.toUpperCase());
        } catch (Exception e) {
            return AudienceInterestStatus.ACTIVE;
        }
    }

    private UUID parseUuidSafe(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return UUID.fromString(text.replace("topic:", "").replace("rec:", ""));
        } catch (Exception e) {
            return null;
        }
    }
}
