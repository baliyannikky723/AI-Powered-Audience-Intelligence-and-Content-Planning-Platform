package com.pulsegpt.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSnapshotRequest {

    @NotBlank(message = "Dataset snapshot name is required")
    private String name;

    private String description;

    @Builder.Default
    private String platform = "YOUTUBE";

    private Instant dateFrom;

    private Instant dateTo;
}
