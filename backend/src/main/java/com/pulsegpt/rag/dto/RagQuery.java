package com.pulsegpt.rag.dto;

import com.pulsegpt.platform.PlatformType;
import com.pulsegpt.rag.model.RagMode;
import lombok.Builder;

import java.util.UUID;

@Builder
public record RagQuery(
        String queryText,
        UUID userId,
        RagMode ragMode,
        PlatformType platform,
        UUID topicId,
        Integer timeRangeDays,
        Integer maxEvidence,
        Boolean includeMemory,
        Boolean includeQuestions,
        Boolean includeTrends,
        Boolean includeContentHistory
) {
    public int getMaxEvidenceOrDefault() {
        return (maxEvidence != null && maxEvidence > 0) ? maxEvidence : 12;
    }

    public RagMode getRagModeOrDefault() {
        return ragMode != null ? ragMode : RagMode.FULL_EVIDENCE_GROUNDED;
    }

    public int getTimeRangeDaysOrDefault() {
        return (timeRangeDays != null && timeRangeDays > 0) ? timeRangeDays : 180;
    }
}
