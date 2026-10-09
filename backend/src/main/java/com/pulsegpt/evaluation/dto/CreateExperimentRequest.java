package com.pulsegpt.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExperimentRequest {

    @NotBlank(message = "Experiment name is required")
    private String experimentName;

    @NotBlank(message = "Experiment type is required")
    private String experimentType;

    private String description;

    @Builder.Default
    private String baselineMode = "BASELINE";

    @Builder.Default
    private String treatmentMode = "EVIDENCE_GROUNDED";

    private UUID datasetSnapshotId;

    private Map<String, Object> parameters;
}
