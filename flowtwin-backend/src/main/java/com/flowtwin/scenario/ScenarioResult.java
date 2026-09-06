package com.flowtwin.scenario;

import java.util.List;
import java.util.Objects;

public record ScenarioResult(
        String name,
        Metrics baseline,
        Metrics scenario,
        Metrics delta,
        List<String> bottlenecks
) {
    /** Coarse key so equivalent scenario outcomes reuse a cached narration. */
    public int hashKey() {
        return Objects.hash(
                name,
                Math.round(baseline.avgTriageWaitMin()), Math.round(scenario.avgTriageWaitMin()),
                Math.round(baseline.p90WaitMin()), Math.round(scenario.p90WaitMin()),
                scenario.peakTriageQueue()
        );
    }
}
