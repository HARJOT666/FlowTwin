package com.flowtwin.ai;

import com.flowtwin.ai.bottleneck.BottleneckPredictionService;
import com.flowtwin.ai.dto.ArrivalForecast;
import com.flowtwin.ai.dto.BottleneckPrediction;
import com.flowtwin.scenario.Metrics;
import com.flowtwin.twin.TwinState;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class BottleneckPredictionServiceTest {
    private final BottleneckPredictionService service = new BottleneckPredictionService();
    private ArrivalForecast forecast(long arrivals) {
        return new ArrivalForecast(arrivals, arrivals, arrivals * 2, arrivals * 4,
                ArrivalForecast.Trend.STABLE, 0, ArrivalForecast.Source.OBSERVED_RATE, 0, Map.of());
    }

    @Test void largeTriageQueueIsCritical() {
        var result = service.predict(new TwinState(40, 30, 5, 2, 40, 12, 10), forecast(10));
        assertThat(result.zone()).isEqualTo(BottleneckPrediction.Zone.TRIAGE);
        assertThat(result.severity()).isEqualTo(BottleneckPrediction.Severity.CRITICAL);
    }

    @Test void nearlyFullBedsAreHighPressure() {
        var result = service.predict(new TwinState(19, 0, 19, 10, 20, 10, 2), forecast(2));
        assertThat(result.zone()).isEqualTo(BottleneckPrediction.Zone.BEDS);
        assertThat(result.severity()).isEqualTo(BottleneckPrediction.Severity.HIGH);
    }

    @Test void normalStateHasNoPredictedBottleneck() {
        var result = service.predict(new TwinState(5, 0, 3, 4, 20, 3, 4), forecast(4));
        assertThat(result.zone()).isEqualTo(BottleneckPrediction.Zone.NONE);
        assertThat(result.severity()).isEqualTo(BottleneckPrediction.Severity.LOW);
    }

    @Test void highArrivalsWithFewDoctorsPredictDoctorPressure() {
        var result = service.predict(new TwinState(10, 0, 3, 10, 100, 1, 30), forecast(30));
        assertThat(result.zone()).isEqualTo(BottleneckPrediction.Zone.DOCTORS);
        assertThat(result.confidence()).isBetween(0.0, 1.0);
    }

    @Test void treatmentNursingPressureCanDominate() {
        var result = service.predict(new TwinState(30, 0, 5, 2, 100, 10, 0), forecast(0));
        assertThat(result.zone()).isEqualTo(BottleneckPrediction.Zone.TREATMENT);
    }

    @Test void noCapacityAndNoDemandIsNotCongestion() {
        var result = service.predict(new TwinState(0, 0, 0, 0, 0, 0, 0), forecast(0));
        assertThat(result.zone()).isEqualTo(BottleneckPrediction.Zone.NONE);
        assertThat(result.pressureRatios().values()).allMatch(Double::isFinite);
    }

    @Test void simulationQueueEvidenceIsIncluded() {
        var result = service.predict(new TwinState(5, 0, 3, 4, 20, 3, 4), forecast(4),
                new Metrics(10, 0, 20, 40, 15));
        assertThat(result.zone()).isEqualTo(BottleneckPrediction.Zone.TRIAGE);
        assertThat(result.pressureRatios().get("TRIAGE")).isEqualTo(5);
    }
}
