package com.flowtwin.service;

import com.flowtwin.gemini.model.ChatResponse;
import com.flowtwin.model.ResourceRole;
import com.flowtwin.model.ScenarioEntity;
import com.flowtwin.repository.ScenarioRepository;
import com.flowtwin.scenario.Metrics;
import com.flowtwin.scenario.ScenarioChange;
import com.flowtwin.scenario.ScenarioRequest;
import com.flowtwin.scenario.ScenarioResponse;
import com.flowtwin.scenario.ScenarioResult;
import com.flowtwin.simulation.SimConfig;
import com.flowtwin.simulation.SimResult;
import com.flowtwin.simulation.SimulationEngine;
import com.flowtwin.simulation.TimelinePoint;
import com.flowtwin.twin.TwinState;
import com.flowtwin.ws.TwinBroadcaster;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot the live twin -> run baseline + scenario simulations -> compute deltas
 * -> narrate with AI -> persist -> broadcast.
 */
@Service
public class ScenarioOrchestrator {

    // Service-time assumptions (a real deployment would learn these from history).
    private static final double MEAN_TRIAGE_MIN = 6.0;
    private static final double MEAN_TREATMENT_MIN = 90.0;
    private static final long SEED = 42L;

    private final TwinStateService twin;
    private final SimulationEngine engine;
    private final NarrationService narration;
    private final ScenarioRepository repository;
    private final TwinBroadcaster broadcaster;

    public ScenarioOrchestrator(TwinStateService twin, SimulationEngine engine,
                                NarrationService narration, ScenarioRepository repository,
                                TwinBroadcaster broadcaster) {
        this.twin = twin;
        this.engine = engine;
        this.narration = narration;
        this.repository = repository;
        this.broadcaster = broadcaster;
    }

    public ScenarioResponse run(ScenarioRequest req) {
        // Step 1: Read the current live twin state.
        TwinState now = twin.snapshot();

        // Step 2: Work out the current arrival rate and the simulation horizon.
        double arrivalRatePerHour = now.observedArrivalRatePerHour();
        int horizonMin = Math.max(1, req.horizonHours()) * 60;

        // Step 3: Build the baseline simulation configuration from the live state.
        SimConfig baselineCfg = new SimConfig(
                horizonMin, arrivalRatePerHour,
                MEAN_TRIAGE_MIN, MEAN_TREATMENT_MIN,
                now.nurses(), now.beds(), now.doctors(), SEED);

        // Step 4: Run the baseline simulation.
        SimResult baselineResult = engine.run(baselineCfg);

        // Steps 5 & 6: Apply the requested changes to build the scenario configuration.
        SimConfig scenarioCfg = applyChanges(baselineCfg, req.changes());

        // Step 7: Run the scenario simulation.
        SimResult scenarioResult = engine.run(scenarioCfg);

        // Step 8: Turn both runs into metrics and compute the difference.
        Metrics baseline = Metrics.from(baselineResult);
        Metrics scenario = Metrics.from(scenarioResult);
        Metrics delta = scenario.minus(baseline);

        ScenarioResult result = new ScenarioResult(
                req.name() == null ? "Scenario" : req.name(),
                baseline, scenario, delta, detectBottlenecks(baselineResult));

        // Step 9: Generate narration (Gemini, or templated fallback), then persist and broadcast.
        ChatResponse narrationResult = narration.narrate(result);

        ScenarioEntity saved = repository.save(new ScenarioEntity(
                result.name(), baseline.p90WaitMin(), scenario.p90WaitMin(),
                narrationResult.summary(), String.join("\n", narrationResult.recommendations())));

        ScenarioResponse response = new ScenarioResponse(saved.getId(), result, narrationResult);
        broadcaster.broadcastInsight(response);
        return response;
    }

    private SimConfig applyChanges(SimConfig baseCfg, List<ScenarioChange> changes) {
        int nurses = baseCfg.nurses();
        int beds = baseCfg.beds();
        int doctors = baseCfg.doctors();

        for (ScenarioChange change : changes) {
            // TODO: honour change.from()/change.to() time windows instead of the whole horizon.
            if (change.role() == ResourceRole.NURSE) {
                nurses += change.delta();
            } else if (change.role() == ResourceRole.DOCTOR) {
                doctors += change.delta();
            } else if (change.role() == ResourceRole.BED || "CAPACITY".equalsIgnoreCase(change.type())) {
                beds += change.delta();
            }
        }

        return new SimConfig(baseCfg.horizonMinutes(), baseCfg.arrivalRatePerHour(),
                baseCfg.meanTriageMinutes(), baseCfg.meanTreatmentMinutes(),
                Math.max(0, nurses), Math.max(0, beds), Math.max(0, doctors), baseCfg.seed());
    }

    private List<String> detectBottlenecks(SimResult r) {
        List<String> out = new ArrayList<>();
        for (TimelinePoint p : r.timeline()) {
            if (p.triageQueue() >= 5) {
                out.add("Triage backlog of " + p.triageQueue() + " at minute " + p.minute());
            }
        }
        return out;
    }
}
