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
@Node("CalendarItem")
public class CalendarItemNode {

    @Id
    private String id;

    private String userId;

    private String title;

    private Instant scheduledAt;

    private String platform;

    private String status;
}
