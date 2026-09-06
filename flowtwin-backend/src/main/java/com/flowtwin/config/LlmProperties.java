package com.flowtwin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flowtwin.llm")
public record LlmProperties(
        String provider,
        String apiKey,
        String baseUrl,
        String model
) {}
