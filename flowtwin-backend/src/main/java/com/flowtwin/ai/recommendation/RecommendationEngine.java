package com.flowtwin.ai.recommendation;

import com.flowtwin.ai.dto.RecommendationScore;
import com.flowtwin.ai.dto.RecommendationScore.Impact;
import com.flowtwin.scenario.Metrics;
import com.flowtwin.scenario.ScenarioResult;
import com.flowtwin.simulation.SimConfig;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RecommendationEngine {
    private static final List<String> LIMITATIONS = List.of(
            "Prototype simulation starts empty and does not model doctor capacity or throughput.",
            "Wait metrics exclude patients still waiting at the horizon; compare equal horizons and assumptions.",
            "Scores compare modeled patient flow, not clinical outcomes or intervention cost.");

    public RecommendationScore rankScenario(ScenarioResult result, SimConfig scenario) {
        // A blocked simulation can misleadingly report zero waits because nobody completes service.
        if (scenario.arrivalRatePerHour() > 0 && (scenario.nurses() <= 0 || scenario.beds() <= 0)) {
            return new RecommendationScore(-100, Impact.NEGATIVE,
                    "Scenario removes required triage or bed capacity; zero completed waits are not an improvement.",
                    Map.of("blockedCapacity", -100.0), LIMITATIONS);
        }
        return rankScenario(result);
    }

    public RecommendationScore rankScenario(ScenarioResult result) {
        Metrics b = result.baseline(), s = result.scenario();
        Map<String, Double> parts = new LinkedHashMap<>();
        parts.put("p90Wait", contribution(b.p90WaitMin(), s.p90WaitMin(), 40));
        parts.put("averageTriageWait", contribution(b.avgTriageWaitMin(), s.avgTriageWaitMin(), 30));
        parts.put("peakTriageQueue", contribution(b.peakTriageQueue(), s.peakTriageQueue(), 20));
        parts.put("averageBedWait", contribution(b.avgBedWaitMin(), s.avgBedWaitMin(), 10));
        double score = Math.round(parts.values().stream().mapToDouble(Double::doubleValue).sum() * 10) / 10.0;
        Impact impact = score < 0 ? Impact.NEGATIVE : score == 0 ? Impact.NONE
                : score >= 30 ? Impact.HIGH : score >= 10 ? Impact.MEDIUM : Impact.LOW;
        String reason = String.format(Locale.ROOT,
                "Weighted wait and queue improvement is %.1f points; projected P90 wait changes from %.1f to %.1f minutes.",
                score, b.p90WaitMin(), s.p90WaitMin());
        return new RecommendationScore(score, impact, reason, parts, LIMITATIONS);
    }

    /** Rank already simulated alternatives; the LLM never chooses or scores the winner. */
    public List<RankedScenario> rankScenarios(List<ScenarioResult> results) {
        return results.stream().map(r -> new RankedScenario(r.name(), rankScenario(r)))
                .sorted(Comparator.comparingDouble((RankedScenario r) -> r.recommendation().score()).reversed())
                .toList();
    }

    public record RankedScenario(String name, RecommendationScore recommendation) {}

    private static double contribution(double baseline, double scenario, double weight) {
        if (!Double.isFinite(baseline) || !Double.isFinite(scenario) || baseline < 0 || scenario < 0) {
            throw new IllegalArgumentException("Recommendation metrics must be finite and nonnegative");
        }
        // One-unit denominator floor handles a zero baseline without inventing a percentage.
        return weight * Math.max(-1, Math.min(1, (baseline - scenario) / Math.max(1, baseline)));
    }
}
