package com.pulsegpt.rag.service;

import com.pulsegpt.audit.AuditService;
import com.pulsegpt.graph.service.KnowledgeGraphQueryService;
import com.pulsegpt.memory.dto.AudienceMemoryContext;
import com.pulsegpt.rag.dto.*;
import com.pulsegpt.rag.model.RagMode;
import com.pulsegpt.recommendation.EvidenceSourceType;
import com.pulsegpt.user.User;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RagQueryService {

    private final VectorRetrievalService vectorRetrievalService;
    private final StructuredRetrievalService structuredRetrievalService;
    private final KnowledgeGraphQueryService knowledgeGraphQueryService;
    private final RagEvidenceFusionService ragEvidenceFusionService;
    private final RagCitationValidator ragCitationValidator;
    private final AuditService auditService;
    private final MeterRegistry meterRegistry;

    public RagQueryService(
            VectorRetrievalService vectorRetrievalService,
            StructuredRetrievalService structuredRetrievalService,
            KnowledgeGraphQueryService knowledgeGraphQueryService,
            RagEvidenceFusionService ragEvidenceFusionService,
            RagCitationValidator ragCitationValidator,
            AuditService auditService,
            MeterRegistry meterRegistry) {
        this.vectorRetrievalService = vectorRetrievalService;
        this.structuredRetrievalService = structuredRetrievalService;
        this.knowledgeGraphQueryService = knowledgeGraphQueryService;
        this.ragEvidenceFusionService = ragEvidenceFusionService;
        this.ragCitationValidator = ragCitationValidator;
        this.auditService = auditService;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(readOnly = true)
    public RagRetrieveResponse retrieveEvidenceOnly(User user, RagQuery query) {
        Timer.Sample sample = Timer.start(meterRegistry);
        RagContext context = buildRagContext(user, query);
        sample.stop(meterRegistry.timer("pulsegpt.rag.retrieval.duration", "mode", query.getRagModeOrDefault().name()));
        meterRegistry.counter("pulsegpt.rag.retrieval", "mode", query.getRagModeOrDefault().name()).increment();

        auditService.logAuditEvent(
                user,
                "RAG_RETRIEVAL_EXECUTED",
                "RAG_QUERY",
                query.queryText(),
                Map.of(
                        "mode", query.getRagModeOrDefault().name(),
                        "evidenceCount", context.evidenceItems().size(),
                        "graphAvailable", context.metadata().graphAvailable(),
                        "latencyMs", context.metadata().retrievalLatencyMs()
                )
        );

        return RagRetrieveResponse.builder()
                .query(query.queryText())
                .evidence(context.evidenceItems())
                .retrievalMetadata(context.metadata())
                .build();
    }

    @Transactional(readOnly = true)
    public RagQueryResponse executeRagQuery(User user, RagQuery query) {
        long startTime = System.currentTimeMillis();
        Timer.Sample sample = Timer.start(meterRegistry);

        RagContext context = buildRagContext(user, query);
        String answer = generateAnswerWithPromptV1(query, context);
        long generationLatencyMs = System.currentTimeMillis() - startTime;

        RagValidationResult validation = ragCitationValidator.validateRagAnswer(
                answer,
                context.evidenceItems(),
                query.getRagModeOrDefault(),
                user.getId()
        );

        List<RagCitationDto> citations = ragCitationValidator.buildCitationDtos(answer, context.evidenceItems());

        sample.stop(meterRegistry.timer("pulsegpt.rag.query.duration", "mode", query.getRagModeOrDefault().name()));
        meterRegistry.counter("pulsegpt.rag.query", "mode", query.getRagModeOrDefault().name()).increment();

        if (!validation.valid()) {
            meterRegistry.counter("pulsegpt.rag.validation.failure").increment();
            auditService.logSecurityEvent(
                    user,
                    "RAG_VALIDATION_FAILED",
                    "RAG_QUERY",
                    query.queryText(),
                    validation.failureSummary() != null ? validation.failureSummary() : "Validation rule failed"
            );
        }

        auditService.logAuditEvent(
                user,
                "RAG_QUERY_EXECUTED",
                "RAG_QUERY",
                query.queryText(),
                Map.of(
                        "mode", query.getRagModeOrDefault().name(),
                        "evidenceCount", context.evidenceItems().size(),
                        "citationsCount", citations.size(),
                        "valid", validation.valid(),
                        "durationMs", generationLatencyMs
                )
        );

        return RagQueryResponse.builder()
                .answer(answer)
                .citations(citations)
                .evidence(context.evidenceItems())
                .retrievalMetadata(context.metadata())
                .validation(validation)
                .generationLatencyMs(generationLatencyMs)
                .build();
    }

    public RagContext buildRagContext(User user, RagQuery query) {
        long startRetrieval = System.currentTimeMillis();
        RagMode mode = query.getRagModeOrDefault();

        List<RagEvidenceItem> vectorComments = new ArrayList<>();
        List<RagEvidenceItem> structuredEvidence = new ArrayList<>();
        AudienceMemoryContext graphMemory = AudienceMemoryContext.empty();
        boolean graphAvailable = true;
        boolean vectorAvailable = true;

        if (mode == RagMode.BASELINE) {
            // Baseline does not retrieve vector embeddings or graph memory
            graphAvailable = false;
            vectorAvailable = false;
            RagRetrievalMetadata meta = RagRetrievalMetadata.builder()
                    .retrievalVersion("v1.0-baseline")
                    .embeddingModel("none")
                    .topK(0)
                    .similarityThreshold(0.0)
                    .evidenceBudget(0)
                    .evidenceCount(0)
                    .graphAvailable(false)
                    .vectorAvailable(false)
                    .sourceCounts(Collections.emptyMap())
                    .retrievalLatencyMs(System.currentTimeMillis() - startRetrieval)
                    .build();

            return RagContext.builder()
                    .query(query)
                    .evidenceItems(Collections.emptyList())
                    .topics(Collections.emptyList())
                    .questions(Collections.emptyList())
                    .comments(Collections.emptyList())
                    .contentHistory(Collections.emptyList())
                    .previousRecommendations(Collections.emptyList())
                    .memoryContext(AudienceMemoryContext.empty())
                    .metadata(meta)
                    .build();
        }

        // 1. Vector Retrieval
        if (mode == RagMode.VECTOR_ONLY || mode == RagMode.FULL_EVIDENCE_GROUNDED) {
            try {
                vectorComments = vectorRetrievalService.retrieveSemanticComments(
                        user.getId(),
                        query.queryText(),
                        query.platform(),
                        query.getTimeRangeDaysOrDefault(),
                        20,
                        0.35
                );
                if (vectorComments.isEmpty()) {
                    meterRegistry.counter("pulsegpt.rag.vector.empty").increment();
                }
            } catch (Exception e) {
                log.warn("Vector retrieval error for user {}: {}", user.getId(), e.getMessage());
                vectorAvailable = false;
            }
        }

        // 2. Structured Retrieval (Topics, Questions, Trends, Content History, Previous Recommendations)
        if (mode == RagMode.GRAPH_AUGMENTED || mode == RagMode.FULL_EVIDENCE_GROUNDED) {
            try {
                structuredEvidence = structuredRetrievalService.retrieveStructuredEvidence(
                        user.getId(),
                        query.topicId(),
                        query.queryText(),
                        query.includeQuestions(),
                        query.includeTrends(),
                        query.includeContentHistory()
                );
            } catch (Exception e) {
                log.warn("Structured retrieval error for user {}: {}", user.getId(), e.getMessage());
            }
        }

        // 3. Neo4j Graph Memory Retrieval
        if ((mode == RagMode.GRAPH_AUGMENTED || mode == RagMode.FULL_EVIDENCE_GROUNDED)
                && !Boolean.FALSE.equals(query.includeMemory())) {
            try {
                graphMemory = knowledgeGraphQueryService.getAudienceMemoryContext(user.getId());
            } catch (Exception e) {
                log.warn("Neo4j graph memory unavailable for user {}: {}", user.getId(), e.getMessage());
                graphAvailable = false;
                meterRegistry.counter("pulsegpt.rag.graph.unavailable").increment();
                graphMemory = AudienceMemoryContext.empty();
            }
        }

        // 4. Evidence Fusion & Balanced Budget Ranking
        List<RagEvidenceItem> fusedEvidence = ragEvidenceFusionService.fuseEvidence(
                vectorComments,
                structuredEvidence,
                graphMemory,
                user.getId(),
                query.getMaxEvidenceOrDefault()
        );

        long retrievalLatency = System.currentTimeMillis() - startRetrieval;

        Map<String, Integer> sourceCounts = new HashMap<>();
        for (RagEvidenceItem item : fusedEvidence) {
            sourceCounts.merge(item.sourceType().name(), 1, Integer::sum);
        }

        RagRetrievalMetadata metadata = RagRetrievalMetadata.builder()
                .retrievalVersion("v1.0-graph-rag")
                .embeddingModel("sentence-transformers/all-MiniLM-L6-v2")
                .topK(20)
                .similarityThreshold(0.35)
                .evidenceBudget(query.getMaxEvidenceOrDefault())
                .evidenceCount(fusedEvidence.size())
                .graphAvailable(graphAvailable)
                .vectorAvailable(vectorAvailable)
                .sourceCounts(sourceCounts)
                .retrievalLatencyMs(retrievalLatency)
                .build();

        return RagContext.builder()
                .query(query)
                .evidenceItems(fusedEvidence)
                .memoryContext(graphMemory)
                .metadata(metadata)
                .build();
    }

    private String generateAnswerWithPromptV1(RagQuery query, RagContext context) {
        if (query.getRagModeOrDefault() == RagMode.BASELINE) {
            return "Based on standard audience profile assumptions, focusing on clear technical fundamentals and tutorials is generally recommended. (Generated in BASELINE mode without evidence grounding)";
        }

        if (context.evidenceItems().isEmpty()) {
            return "Insufficient audience evidence found in your workspace to answer this query. Ingest audience comments or run topic clustering to build verified evidence.";
        }

        // Construct Evidence Grounded synthesis using PROMPT_RAG_V1 structure
        StringBuilder sb = new StringBuilder();
        sb.append("Based on the analyzed audience evidence for \"").append(query.queryText()).append("\":\n\n");

        // Highlight active topics/memory
        Optional<RagEvidenceItem> memoryEvidence = context.evidenceItems().stream()
                .filter(e -> e.sourceType() == EvidenceSourceType.MEMORY)
                .findFirst();

        Optional<RagEvidenceItem> topicEvidence = context.evidenceItems().stream()
                .filter(e -> e.sourceType() == EvidenceSourceType.TOPIC)
                .findFirst();

        Optional<RagEvidenceItem> questionEvidence = context.evidenceItems().stream()
                .filter(e -> e.sourceType() == EvidenceSourceType.QUESTION)
                .findFirst();

        Optional<RagEvidenceItem> commentEvidence = context.evidenceItems().stream()
                .filter(e -> e.sourceType() == EvidenceSourceType.COMMENT)
                .findFirst();

        if (memoryEvidence.isPresent()) {
            sb.append("• **Audience Interest**: ").append(memoryEvidence.get().text())
                    .append(" ").append(memoryEvidence.get().citationId()).append("\n");
        } else if (topicEvidence.isPresent()) {
            sb.append("• **Key Focus Topic**: ").append(topicEvidence.get().text())
                    .append(" ").append(topicEvidence.get().citationId()).append("\n");
        }

        if (questionEvidence.isPresent()) {
            sb.append("• **Top Inquired Need**: ").append(questionEvidence.get().text())
                    .append(" ").append(questionEvidence.get().citationId()).append("\n");
        }

        if (commentEvidence.isPresent()) {
            sb.append("• **Direct Feedback Observation**: \"").append(commentEvidence.get().text())
                    .append("\" ").append(commentEvidence.get().citationId()).append("\n");
        }

        sb.append("\n**Strategic Recommendation**:\n");
        sb.append("Create targeted content addressing these specific verified inquiries. Ground your explanation in concrete examples that directly resolve the audience's recurring questions.");

        return sb.toString();
    }
}
