package com.flowtwin.scenario;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ScenarioRequest(
        String name,
        @NotEmpty List<ScenarioChange> changes,
        int horizonHours
) {}
