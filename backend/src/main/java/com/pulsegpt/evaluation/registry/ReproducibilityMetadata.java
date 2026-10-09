package com.pulsegpt.evaluation.registry;

import lombok.Builder;
import lombok.Value;

import java.util.HashMap;
import java.util.Map;

@Value
@Builder
public class ReproducibilityMetadata {

    @Builder.Default
    String modelName = "pulsegpt-production-v1";

    @Builder.Default
    String modelVersion = "2026-10";

    @Builder.Default
    String promptVersion = "PROMPT_RAG_V1";

    @Builder.Default
    String algorithmName = "HDBSCAN";

    @Builder.Default
    String algorithmVersion = "0.8.38";

    @Builder.Default
    String embeddingModel = "sentence-transformers/all-MiniLM-L6-v2";

    @Builder.Default
    int embeddingDimension = 384;

    @Builder.Default
    int randomSeed = 42;

    @Builder.Default
    String memoryVersion = "2026-10-v1";

    @Builder.Default
    String confidenceFormulaVersion = "CANONICAL_EXP_V1";

    @Builder.Default
    String decayFormulaVersion = "EXP_HALF_LIFE_V1";

    @Builder.Default
    int halfLifeDays = 45;

    @Builder.Default
    int evidenceWindowDays = 180;

    @Builder.Default
    String projectionMode = "INCREMENTAL";

    @Builder.Default
    int graphNodeCount = 0;

    @Builder.Default
    int graphRelationshipCount = 0;

    @Builder.Default
    String ragVersion = "v1.0-graph-rag";

    @Builder.Default
    String retrievalVersion = "v1.0-graph-rag";

    @Builder.Default
    double similarityThreshold = 0.35;

    @Builder.Default
    int evidenceBudget = 12;

    public static ReproducibilityMetadata defaults() {
        return ReproducibilityMetadata.builder().build();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("modelName", modelName);
        map.put("modelVersion", modelVersion);
        map.put("promptVersion", promptVersion);
        map.put("algorithmName", algorithmName);
        map.put("algorithmVersion", algorithmVersion);
        map.put("embeddingModel", embeddingModel);
        map.put("embeddingDimension", embeddingDimension);
        map.put("randomSeed", randomSeed);
        map.put("memoryVersion", memoryVersion);
        map.put("confidenceFormulaVersion", confidenceFormulaVersion);
        map.put("decayFormulaVersion", decayFormulaVersion);
        map.put("halfLifeDays", halfLifeDays);
        map.put("evidenceWindowDays", evidenceWindowDays);
        map.put("projectionMode", projectionMode);
        map.put("graphNodeCount", graphNodeCount);
        map.put("graphRelationshipCount", graphRelationshipCount);
        map.put("ragVersion", ragVersion);
        map.put("retrievalVersion", retrievalVersion);
        map.put("similarityThreshold", similarityThreshold);
        map.put("evidenceBudget", evidenceBudget);
        return map;
    }
}
