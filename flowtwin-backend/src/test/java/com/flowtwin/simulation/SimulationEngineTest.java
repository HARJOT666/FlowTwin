package com.flowtwin.simulation;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulationEngineTest {
    private final SimulationEngine engine = new SimulationEngine();

    @Test void sameSeedAndInputProduceIdenticalResults() {
        SimConfig config = new SimConfig(240, 20, 6, 45, 3, 8, 2, 42, 4, 5, 14);
        assertThat(engine.run(config)).isEqualTo(engine.run(config));
    }

    @Test void currentTwinQueuesAndOccupancySeedTheTimeline() {
        SimResult result = engine.run(new SimConfig(60, 0, 6, 45, 0, 10, 0, 42, 6, 4, 12));
        assertThat(result.totalPatients()).isEqualTo(12);
        assertThat(result.timeline().get(0).triageQueue()).isEqualTo(6);
        assertThat(result.timeline().get(0).treatmentQueue()).isEqualTo(2);
        assertThat(result.timeline().get(0).bedsOccupied()).isEqualTo(4);
    }

    @Test void treatmentRequiresBothDoctorAndBedCapacity() {
        SimConfig noDoctors = new SimConfig(180, 12, 6, 40, 3, 10, 0, 7);
        SimConfig withDoctors = new SimConfig(180, 12, 6, 40, 3, 10, 3, 7);
        SimResult blocked = engine.run(noDoctors);
        SimResult staffed = engine.run(withDoctors);
        assertThat(blocked.completedPatients()).isZero();
        assertThat(blocked.peakTreatmentQueue()).isGreaterThan(0);
        assertThat(staffed.completedPatients()).isGreaterThan(0);
        assertThat(staffed.peakTreatmentQueue()).isLessThan(blocked.peakTreatmentQueue());
    }

    @Test void emptyDepartmentWithZeroArrivalsStaysEmpty() {
        SimResult result = engine.run(new SimConfig(60, 0, 6, 40, 3, 10, 2, 1));
        assertThat(result.totalPatients()).isZero();
        assertThat(result.completedPatients()).isZero();
        assertThat(result.bedUtilizationPct()).isZero();
    }

    @Test void singletonEngineKeepsConcurrentRunsIsolated() {
        SimConfig a = new SimConfig(120, 10, 6, 40, 2, 5, 1, 11);
        SimConfig b = new SimConfig(120, 30, 6, 40, 4, 12, 4, 99);
        SimResult expectedA = engine.run(a);
        SimResult expectedB = engine.run(b);
        var futureA = CompletableFuture.supplyAsync(() -> engine.run(a));
        var futureB = CompletableFuture.supplyAsync(() -> engine.run(b));
        assertThat(futureA.join()).isEqualTo(expectedA);
        assertThat(futureB.join()).isEqualTo(expectedB);
    }

    @Test void invalidSimulationInputIsRejected() {
        assertThatThrownBy(() -> engine.run(new SimConfig(0, 10, 6, 40, 2, 5, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
