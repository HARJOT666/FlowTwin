package com.flowtwin.narration;

import com.flowtwin.scenario.Metrics;
import com.flowtwin.scenario.ScenarioResult;
import org.springframework.stereotype.Component;

/**
 * Builds a GROUNDED prompt: the model is given only the simulation's own numbers and is
 * asked to explain/prioritise them - never to invent figures. This is the guardrail that
 * keeps narration trustworthy.
 */
@Component
public class PromptBuilder {

    public record Prompt(String system, String user) {}

    private static final String SYSTEM = """
            You are an operations assistant for a hospital emergency department.
            You ONLY use the numbers provided by the user. Never invent or estimate figures.
            Be concise and practical. Respond in this exact shape:
            - Line 1: a single-sentence summary of the scenario's impact.
            - Then 2-4 recommendation lines, each starting with "- ".
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
