package com.pulsegpt.production.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductionDraftDto(
        ContentBriefDto brief,
        ScriptOutlineDto outline,
        List<HookVariantDto> hooks,
        List<TitleVariantDto> titles,
        List<CtaVariantDto> ctas,
        ThumbnailPromptDto thumbnail,
        ProductionChecklistDto checklist,
        List<String> evidenceIds
) {
}
