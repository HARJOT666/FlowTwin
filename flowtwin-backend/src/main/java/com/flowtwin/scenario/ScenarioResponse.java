package com.flowtwin.scenario;

import com.flowtwin.narration.NarrationResult;

public record ScenarioResponse(Long id, ScenarioResult result, NarrationResult narration) {}
