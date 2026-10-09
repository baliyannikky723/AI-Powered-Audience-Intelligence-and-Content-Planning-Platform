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
@Node("Audience")
public class AudienceNode {

    @Id
    private String id;

    private String userId;

    private Instant createdAt;

    private Instant updatedAt;
}
