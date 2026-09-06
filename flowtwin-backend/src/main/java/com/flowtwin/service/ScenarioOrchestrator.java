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
        TwinState now = twin.snapshot();
        int horizonMin = Math.max(1, req.horizonHours()) * 60;

        SimConfig baselineCfg = new SimConfig(
                horizonMin, now.observedArrivalRatePerHour(),
                MEAN_TRIAGE_MIN, MEAN_TREATMENT_MIN,
                now.nurses(), now.beds(), now.doctors(), SEED);

        SimConfig scenarioCfg = applyChanges(baselineCfg, req.changes());

        SimResult baseRes = engine.run(baselineCfg);
        SimResult scenRes = engine.run(scenarioCfg);

        Metrics baseline = Metrics.from(baseRes);
        Metrics scenario = Metrics.from(scenRes);
        Metrics delta = scenario.minus(baseline);

        ScenarioResult result = new ScenarioResult(
                req.name() == null ? "Scenario" : req.name(),
                baseline, scenario, delta, detectBottlenecks(baseRes));

        ChatResponse narr = narration.narrate(result);

        ScenarioEntity saved = repository.save(new ScenarioEntity(
                result.name(), baseline.p90WaitMin(), scenario.p90WaitMin(),
                narr.summary(), String.join("\n", narr.recommendations())));

        ScenarioResponse response = new ScenarioResponse(saved.getId(), result, narr);
        broadcaster.broadcastInsight(response);
        return response;
    }

    private SimConfig applyChanges(SimConfig c, List<ScenarioChange> changes) {
        int nurses = c.nurses(), beds = c.beds(), doctors = c.doctors();
        for (ScenarioChange ch : changes) {
            // TODO: honour ch.from()/ch.to() time windows instead of applying for the whole horizon.
            if (ch.role() == ResourceRole.NURSE) nurses += ch.delta();
            else if (ch.role() == ResourceRole.DOCTOR) doctors += ch.delta();
            else if (ch.role() == ResourceRole.BED || "CAPACITY".equalsIgnoreCase(ch.type())) beds += ch.delta();
        }
        return new SimConfig(c.horizonMinutes(), c.arrivalRatePerHour(),
                c.meanTriageMinutes(), c.meanTreatmentMinutes(),
                Math.max(0, nurses), Math.max(0, beds), Math.max(0, doctors), c.seed());
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
