package com.flowtwin.gemini.model;

import java.util.List;

/**
 * FlowTwin's clean, application-level AI narration result. This is the ONLY AI type the rest of
 * the application depends on - controllers and the scenario layer never see Gemini's raw
 * {@link GeminiResponse}.
 *
 * @param summary         one-sentence impact summary grounded in the simulation numbers
 * @param recommendations 2-4 actionable operational recommendations
 * @param tradeoffs       important tradeoffs of acting on the recommendations (may be empty)
 * @param source          which path produced this result: {@code "gemini"}, {@code "gemini-cached"}, or {@code "fallback"}
 */
public record ChatResponse(
        String summary,
        List<String> recommendations,
        List<String> tradeoffs,
        String source
) {}
