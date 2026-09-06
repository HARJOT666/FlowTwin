package com.flowtwin.gemini;

import com.flowtwin.scenario.Metrics;
import com.flowtwin.scenario.ScenarioResult;
import org.springframework.stereotype.Component;

/**
 * Builds the GROUNDED Gemini prompt from simulation metrics (baseline, scenario, delta,
 * bottlenecks). The model is handed only the simulation's own numbers and asked to explain and
 * prioritise them - never to invent figures. This is the guardrail that keeps narration trustworthy.
 */
@Component
public class GeminiPromptBuilder {

    public record Prompt(String system, String user) {}

    private static final String SYSTEM = """
            You are an operations assistant for a hospital emergency department.
            The numbers you are given come from FlowTwin's discrete-event simulation engine - NOT
            from a live hospital. You do NOT run the simulation; you only explain and prioritise
            the values supplied to you.

            Rules:
            - Use ONLY the numbers provided by the user. Never invent, estimate, or extrapolate figures.
            - Do not claim access to live hospital data beyond the supplied values.
            - Base every numerical statement solely on the supplied data.
            - If the data looks unrealistic or internally inconsistent, say so and note that it is a
              simulation result - do not silently "correct" it.

            Return your answer as JSON with these fields:
            - "summary": one concise sentence describing the scenario's impact vs. baseline.
            - "recommendations": 2 to 4 concrete, actionable operational recommendations.
            - "tradeoffs": the important tradeoffs of acting on those recommendations.
            """;

    public Prompt build(ScenarioResult r) {
        Metrics b = r.baseline();
        Metrics s = r.scenario();
        Metrics d = r.delta();

        StringBuilder sb = new StringBuilder();
        sb.append("Scenario: ").append(r.name()).append("\n\n");
        sb.append("Baseline vs scenario:\n");
        sb.append(line("Avg triage wait (min)", b.avgTriageWaitMin(), s.avgTriageWaitMin(), d.avgTriageWaitMin()));
        sb.append(line("Avg wait for bed (min)", b.avgBedWaitMin(), s.avgBedWaitMin(), d.avgBedWaitMin()));
        sb.append(line("P90 total wait (min)", b.p90WaitMin(), s.p90WaitMin(), d.p90WaitMin()));
        sb.append("Peak triage queue: ").append(b.peakTriageQueue()).append(" -> ").append(s.peakTriageQueue()).append("\n");
        sb.append("Bed utilization %: ").append(b.bedUtilizationPct()).append(" -> ").append(s.bedUtilizationPct()).append("\n");

        if (!r.bottlenecks().isEmpty()) {
            sb.append("\nObserved bottlenecks (baseline):\n");
            r.bottlenecks().stream().limit(6).forEach(x -> sb.append("- ").append(x).append("\n"));
        }
        sb.append("\nExplain the impact and recommend next best actions, using only these numbers.");
        return new Prompt(SYSTEM, sb.toString());
    }

    private static String line(String label, double base, double scen, double delta) {
        String tag = delta < 0 ? " (improved)" : delta > 0 ? " (worse)" : "";
        return "- " + label + ": " + base + " -> " + scen + " (delta " + delta + ")" + tag + "\n";
    }
}
