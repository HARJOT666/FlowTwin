package com.flowtwin.narration;

import com.flowtwin.gemini.model.ChatResponse;
import com.flowtwin.scenario.ScenarioResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic narration built purely from the numbers. Safety net for when Gemini is
 * unavailable or fails - Gemini is the intended path; this only prevents a blank card and
 * guarantees the app always responds. Produces a {@link ChatResponse} with source "fallback".
 */
public final class TemplatedFallback {

    private TemplatedFallback() { }

    public static ChatResponse build(ScenarioResult r) {
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

        return new ChatResponse(summary, recs, List.of(), "fallback");
    }
}
