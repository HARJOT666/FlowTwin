package com.flowtwin.scenario;

import com.flowtwin.ai.dto.RecommendationScore;
import com.flowtwin.gemini.model.ChatResponse;

public record ScenarioResponse(Long id, ScenarioResult result, ChatResponse narration,
                               RecommendationScore recommendation) {
    public ScenarioResponse(Long id, ScenarioResult result, ChatResponse narration) {
        this(id, result, narration, null);
    }
}
