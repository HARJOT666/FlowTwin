package com.flowtwin.scenario;

import com.flowtwin.gemini.model.ChatResponse;

public record ScenarioResponse(Long id, ScenarioResult result, ChatResponse narration) {}
