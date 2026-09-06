package com.flowtwin.ai;

import com.flowtwin.ai.dto.RecommendationScore.Impact;
import com.flowtwin.ai.recommendation.RecommendationEngine;
import com.flowtwin.scenario.*;
import com.flowtwin.simulation.SimConfig;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class RecommendationEngineTest {
    private final RecommendationEngine engine = new RecommendationEngine();
    private final Metrics baseline = new Metrics(20, 30, 60, 20, 90);
    private ScenarioResult result(Metrics scenario) {
        return new ScenarioResult("Extra nurse", baseline, scenario, scenario.minus(baseline), List.of());
    }

    @Test void beneficialScenarioHasExplainablePositiveScore() {
        var score = engine.rankScenario(result(new Metrics(10, 15, 30, 10, 80)));
        assertThat(score.score()).isEqualTo(40);
        assertThat(score.impact()).isEqualTo(Impact.HIGH);
        assertThat(score.weightedContributions().values().stream().mapToDouble(Double::doubleValue).sum()).isEqualTo(40);
    }

    @Test void neutralScenarioScoresZero() {
        var score = engine.rankScenario(result(baseline));
        assertThat(score.score()).isZero();
        assertThat(score.impact()).isEqualTo(Impact.NONE);
    }

    @Test void worseScenarioHasNegativeScore() {
        var score = engine.rankScenario(result(new Metrics(40, 60, 120, 40, 100)));
        assertThat(score.score()).isEqualTo(-80);
        assertThat(score.impact()).isEqualTo(Impact.NEGATIVE);
    }

    @Test void zeroBaselinePenalizesNewWaitsWithoutDivisionByZero() {
        Metrics zero = new Metrics(0, 0, 0, 0, 0);
        var score = engine.rankScenario(new ScenarioResult("Worse", zero, baseline, baseline, List.of()));
        assertThat(score.score()).isEqualTo(-80);
    }

    @Test void utilizationAloneDoesNotInventBenefit() {
        assertThat(engine.rankScenario(result(new Metrics(20, 30, 60, 20, 50))).score()).isZero();
    }

    @Test void alternativesAreOrderedByCalculatedScore() {
        var ranked = engine.rankScenarios(List.of(result(baseline), result(new Metrics(10, 15, 30, 10, 80))));
        assertThat(ranked.get(0).recommendation().score()).isEqualTo(40);
        assertThat(ranked.get(1).recommendation().score()).isZero();
    }

    @Test void blockedSimulationCannotLookBeneficial() {
        var score = engine.rankScenario(result(new Metrics(0, 0, 0, 0, 0)),
                new SimConfig(240, 20, 6, 90, 0, 20, 3, 42));
        assertThat(score.score()).isEqualTo(-100);
        assertThat(score.primaryReason()).contains("zero completed waits are not an improvement");
    }

    @Test void invalidMetricsAreRejected() {
        assertThatThrownBy(() -> engine.rankScenario(result(new Metrics(Double.NaN, 0, 0, 0, 0))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
