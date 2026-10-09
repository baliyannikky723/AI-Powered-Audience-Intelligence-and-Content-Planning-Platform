package com.pulsegpt.ai.client;

import com.pulsegpt.ai.client.dto.*;

public interface AiServiceClient {

    AiHealthResponse getHealth();

    AiCommentProcessResponse processComment(AiCommentProcessRequest request);

    AiBatchCommentProcessResponse processBatch(AiBatchCommentProcessRequest request);

    AiEmbedResponse generateEmbeddings(AiEmbedRequest request);

    AiClusteringRunResponse clusterComments(AiClusteringRunRequest request);

    AiRecommendationGenerateResponse generateRecommendations(AiRecommendationGenerateRequest request);

    AiRecommendationRepairResponse repairRecommendation(AiRecommendationRepairRequest request);

    AiProductionGenerateResponse generateProductionDraft(AiProductionGenerateRequest request);

    AiProductionRepairResponse repairProductionDraft(AiProductionRepairRequest request);
}

