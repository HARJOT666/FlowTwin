package com.flowtwin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI narration configuration. The active provider is selected by {@code flowtwin.ai.provider}.
 * Secrets (the Gemini API key) come from the environment via {@code ${GEMINI_API_KEY:}} and are
 * never logged or returned in API responses.
 */
@ConfigurationProperties(prefix = "flowtwin.ai")
public record AiProperties(
        String provider,
        Gemini gemini
) {
    public AiProperties {
        if (provider == null || provider.isBlank()) provider = "gemini";
        if (gemini == null) gemini = new Gemini(null, null, null, null);
    }

    public record Gemini(
            String apiKey,
            String model,
            String baseUrl,
            Integer timeoutMs
    ) {
        public Gemini {
            if (model == null || model.isBlank()) model = "gemini-3.5-flash";
            if (baseUrl == null || baseUrl.isBlank())
                baseUrl = "https://generativelanguage.googleapis.com/v1beta";
            if (timeoutMs == null || timeoutMs <= 0) timeoutMs = 20_000;
        }

        /** True only when an API key is actually configured. */
        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
