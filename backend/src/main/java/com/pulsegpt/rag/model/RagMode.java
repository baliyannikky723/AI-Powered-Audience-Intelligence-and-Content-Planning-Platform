package com.pulsegpt.rag.model;

public enum RagMode {
    BASELINE,
    VECTOR_ONLY,
    GRAPH_AUGMENTED,
    FULL_EVIDENCE_GROUNDED;

    public static RagMode fromString(String mode) {
        if (mode == null) return FULL_EVIDENCE_GROUNDED;
        try {
            return RagMode.valueOf(mode.toUpperCase());
        } catch (IllegalArgumentException e) {
            if ("EVIDENCE_GROUNDED".equalsIgnoreCase(mode)) {
                return FULL_EVIDENCE_GROUNDED;
            }
            return FULL_EVIDENCE_GROUNDED;
        }
    }
}
