package com.pulsegpt.graph.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Node("Topic")
public class TopicNode {

    @Id
    private String id;

    private String userId;

    private String topicId;

    private String label;

    private String status;

    private Double confidence;

    private Instant firstSeenAt;

    private Instant lastSeenAt;

    private Integer evidenceCount;

    private Instant createdAt;

    private Instant updatedAt;
}
