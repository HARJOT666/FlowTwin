package com.flowtwin.narration;

import com.flowtwin.scenario.ScenarioResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic explanation built purely from the numbers. Demo safety net for when the
 * LLM is unreachable - the AI path is the intended one; this only prevents a blank card.
 */
public final class TemplatedFallback {

    private TemplatedFallback() { }

    public static NarrationResult build(ScenarioResult r) {
        double p90Delta = r.delta().p90WaitMin();
        String direction = p90Delta < 0 ? "reduces" : p90Delta > 0 ? "increases" : "does not change";
        String summary = String.format(
                "%s %s projected P90 wait by %.1f min (%.1f -> %.1f).",
                r.name(), direction, Math.abs(p90Delta),
                r.baseline().p90WaitMin(), r.scenario().p90WaitMin());

        List<String> recs = new ArrayList<>();
        if (r.scenario().peakTriageQueue() >= 5)
            recs.add("Triage remains a bottleneck; consider adding triage capacity.");
        if (r.scenario().bedUtilizationPct() >= 90)
            recs.add("Beds are near saturation; expand bed capacity or speed discharge.");
        if (recs.isEmpty())
            recs.add("Metrics are within normal range for this horizon.");

        return new NarrationResult(summary, recs, "fallback");
    }
}
