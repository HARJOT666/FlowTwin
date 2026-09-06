package com.flowtwin.gemini.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Java model of the RAW Gemini {@code generateContent} response. This type stays inside the gemini
 * package - {@link com.flowtwin.gemini.GeminiService} converts it into {@link ChatResponse} before
 * anything else in the application sees it. Unknown fields (usage metadata, model version, safety
 * ratings, ...) are ignored so Gemini can evolve its payload without breaking us.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiResponse(
        List<Candidate> candidates,
        PromptFeedback promptFeedback
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content, String finishReason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Content(List<Part> parts, String role) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Part(String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PromptFeedback(String blockReason) {}

    /** The concatenated text of the first candidate, or an empty string when there is none. */
    public String firstText() {
        if (candidates == null || candidates.isEmpty()) return "";
        Candidate first = candidates.get(0);
        if (first == null || first.content() == null || first.content().parts() == null) return "";
        StringBuilder sb = new StringBuilder();
        for (Part p : first.content().parts()) {
            if (p != null && p.text() != null) sb.append(p.text());
        }
        return sb.toString();
    }

    /** Reason the prompt was blocked, if any (e.g. safety), otherwise an empty string. */
    public String blockReason() {
        return promptFeedback == null || promptFeedback.blockReason() == null
                ? "" : promptFeedback.blockReason();
    }
}
