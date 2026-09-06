package com.flowtwin.gemini.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Java model of the Gemini {@code generateContent} request body. Models only the fields FlowTwin
 * actually sends. Null fields (e.g. the system instruction has no {@code role}) are omitted so the
 * serialized JSON matches Gemini's expected shape.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeminiRequest(
        Content systemInstruction,
        List<Content> contents,
        GenerationConfig generationConfig
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Content(String role, List<Part> parts) {
        public static Content of(String role, String text) {
            return new Content(role, List.of(new Part(text)));
        }
    }

    public record Part(String text) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GenerationConfig(Double temperature, String responseMimeType, Map<String, Object> responseSchema) {}

    /**
     * Builds a grounded narration request: the system prompt goes into {@code systemInstruction},
     * the metrics into the user turn, and a response schema pins structured JSON output so the
     * reply maps straight onto {@link ChatResponse}.
     */
    public static GeminiRequest narration(String system, String user) {
        Map<String, Object> schema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "summary", Map.of("type", "STRING"),
                        "recommendations", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                        "tradeoffs", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))),
                "required", List.of("summary", "recommendations"));

        return new GeminiRequest(
                new Content(null, List.of(new Part(system))),   // systemInstruction: no role
                List.of(Content.of("user", user)),
                new GenerationConfig(0.2, "application/json", schema));
    }
}
