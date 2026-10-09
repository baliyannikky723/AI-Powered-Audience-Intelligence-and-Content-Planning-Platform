package com.pulsegpt.memory.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.memory")
public class AudienceMemoryProperties {

    private boolean enabled = true;
    private int halfLifeDays = 45;
    private int evidenceWindowDays = 180;
    private double activeThreshold = 0.60;
    private double weakeningThreshold = 0.30;
    private double maxConfidence = 0.99;
    private double confidenceScaleFactor = 200.0;
}
