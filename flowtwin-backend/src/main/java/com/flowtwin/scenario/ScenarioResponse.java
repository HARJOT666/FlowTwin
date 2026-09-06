package com.flowtwin.scenario;

import com.flowtwin.narration.NarrationResult;
import com.flowtwin.ai.dto.RecommendationScore;

public record ScenarioResponse(Long id, ScenarioResult result, NarrationResult narration,
                               RecommendationScore recommendation) {
    public ScenarioResponse(Long id, ScenarioResult result, NarrationResult narration) {
        this(id, result, narration, null);
    }
}
