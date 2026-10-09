package com.pulsegpt.evaluation.health;

import com.pulsegpt.ai.client.AiServiceClient;
import com.pulsegpt.ai.client.dto.AiHealthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiServiceHealthIndicator implements HealthIndicator {

    private final AiServiceClient aiServiceClient;

    @Override
    public Health health() {
        try {
            AiHealthResponse response = aiServiceClient.getHealth();
            if ("UP".equalsIgnoreCase(response.status())) {
                return Health.up()
                        .withDetail("service", response.service())
                        .withDetail("version", response.version())
                        .withDetail("modelsLoaded", response.modelsLoaded())
                        .build();
            } else {
                return Health.down()
                        .withDetail("service", response.service())
                        .withDetail("reason", "Service status is " + response.status())
                        .build();
            }
        } catch (Exception ex) {
            log.warn("AI service health check probe failed: {}", ex.getMessage());
            return Health.down()
                    .withDetail("error", "AI service unreachable")
                    .build();
        }
    }
}
