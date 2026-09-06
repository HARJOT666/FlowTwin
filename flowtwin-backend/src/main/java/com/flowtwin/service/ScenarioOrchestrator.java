package com.flowtwin.service;

import com.flowtwin.ai.dto.AiInsight;
import com.flowtwin.ai.dto.RecommendationScore;
import com.flowtwin.ai.recommendation.RecommendationEngine;
import com.flowtwin.ai.service.AiInsightService;
import com.flowtwin.gemini.model.ChatResponse;
import com.flowtwin.model.ResourceRole;
import com.flowtwin.model.ScenarioEntity;
import com.flowtwin.repository.ScenarioRepository;
import com.flowtwin.scenario.*;
import com.flowtwin.simulation.*;
import com.flowtwin.twin.TwinState;
import com.flowtwin.ws.TwinBroadcaster;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Snapshot -> simulate -> score -> narrate -> persist -> broadcast. */
@Service
public class ScenarioOrchestrator {
    private static final double MEAN_TRIAGE_MIN = 6.0;
    private static final double MEAN_TREATMENT_MIN = 90.0;
    private static final long SEED = 42L;
    private final TwinStateService twin;
    private final SimulationEngine engine;
    private final NarrationService narration;
    private final ScenarioRepository repository;
    private final TwinBroadcaster broadcaster;
    private final RecommendationEngine recommendations;
    private final AiInsightService insights;

    public ScenarioOrchestrator(TwinStateService twin, SimulationEngine engine,
                                NarrationService narration, ScenarioRepository repository,
                                TwinBroadcaster broadcaster, RecommendationEngine recommendations,
                                AiInsightService insights) {
        this.twin = twin;
        this.engine = engine;
        this.narration = narration;
        this.repository = repository;
        this.broadcaster = broadcaster;
        this.recommendations = recommendations;
        this.insights = insights;
    }

    public ScenarioResponse run(ScenarioRequest request) {
        TwinState now = twin.snapshot();
        int horizonMinutes = Math.max(1, request.horizonHours()) * 60;
        SimConfig baselineConfig = new SimConfig(horizonMinutes, now.observedArrivalRatePerHour(),
                MEAN_TRIAGE_MIN, MEAN_TREATMENT_MIN, now.nurses(), now.beds(), now.doctors(), SEED,
                now.triageQueue(), now.bedsOccupied(), now.patientsInDept());
        SimConfig scenarioConfig = applyChanges(baselineConfig, request.changes());
        SimResult baselineResult = engine.run(baselineConfig);
        SimResult scenarioResult = engine.run(scenarioConfig);
        Metrics baseline = Metrics.from(baselineResult);
        Metrics scenario = Metrics.from(scenarioResult);
        ScenarioResult result = new ScenarioResult(request.name() == null ? "Scenario" : request.name(),
                baseline, scenario, scenario.minus(baseline), detectBottlenecks(baselineResult));
        RecommendationScore recommendation = recommendations.rankScenario(result, scenarioConfig);
        AiInsight insight = insights.insights(now, baseline);
        ChatResponse narrated = narration.narrate(result, insight, recommendation);
        ScenarioEntity saved = repository.save(new ScenarioEntity(result.name(), baseline.p90WaitMin(),
                scenario.p90WaitMin(), narrated.summary(), String.join("\n", narrated.recommendations())));
        ScenarioResponse response = new ScenarioResponse(saved.getId(), result, narrated, recommendation);
        broadcaster.broadcastInsight(response);
        return response;
    }

    private SimConfig applyChanges(SimConfig config, List<ScenarioChange> changes) {
        int nurses = config.nurses(), beds = config.beds(), doctors = config.doctors();
        for (ScenarioChange change : changes) {
            if (change.role() == ResourceRole.NURSE) nurses += change.delta();
            else if (change.role() == ResourceRole.DOCTOR) doctors += change.delta();
            else if (change.role() == ResourceRole.BED || "CAPACITY".equalsIgnoreCase(change.type())) beds += change.delta();
        }
        return new SimConfig(config.horizonMinutes(), config.arrivalRatePerHour(),
                config.meanTriageMinutes(), config.meanTreatmentMinutes(), Math.max(0, nurses),
                Math.max(0, beds), Math.max(0, doctors), config.seed(), config.initialTriageQueue(),
                Math.min(Math.max(0, beds), config.initialBedsOccupied()), config.initialPatientsInDept());
    }

    private List<String> detectBottlenecks(SimResult result) {
        List<String> bottlenecks = new ArrayList<>();
        for (TimelinePoint point : result.timeline()) {
            if (point.triageQueue() >= 5)
                bottlenecks.add("Triage backlog of " + point.triageQueue() + " at minute " + point.minute());
            if (point.treatmentQueue() >= 5)
                bottlenecks.add("Treatment backlog of " + point.treatmentQueue() + " at minute " + point.minute());
        }
        return bottlenecks;
    }
}
