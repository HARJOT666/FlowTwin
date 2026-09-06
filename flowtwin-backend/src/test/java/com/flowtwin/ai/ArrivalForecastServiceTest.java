package com.flowtwin.ai;

import com.flowtwin.ai.dto.ArrivalForecast;
import com.flowtwin.ai.forecast.ArrivalForecastService;
import com.flowtwin.model.*;
import com.flowtwin.repository.PatientEventRepository;
import com.flowtwin.twin.TwinState;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ArrivalForecastServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-06T10:00:00Z");
    private final PatientEventRepository events = mock(PatientEventRepository.class);
    private final ArrivalForecastService service = new ArrivalForecastService(events);

    private TwinState state(double rate) { return new TwinState(0, 0, 0, 4, 20, 3, rate); }

    private void history(long a, long b, long c, long d) {
        when(events.countByTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(any(), any(), any()))
                .thenReturn(a, b, c, d);
        when(events.findFirstByTypeOrderByOccurredAtAsc(EventType.PATIENT_ARRIVED))
                .thenReturn(Optional.of(new PatientEvent("history", EventType.PATIENT_ARRIVED,
                        Zone.TRIAGE, Acuity.L3, NOW.minus(5, ChronoUnit.HOURS))));
    }

    @Test void risingArrivalsAreDampedAndCumulative() {
        history(8, 12, 20, 28);
        ArrivalForecast f = service.forecast(state(28), NOW);
        assertThat(f.trend()).isEqualTo(ArrivalForecast.Trend.RISING);
        assertThat(f.source()).isEqualTo(ArrivalForecast.Source.HISTORY_BLEND);
        assertThat(f.predictedArrivalsNextHour()).isEqualTo(28);
        assertThat(f.predictedArrivalsNext2Hours()).isEqualTo(59);
        assertThat(f.predictedArrivalsNext4Hours()).isEqualTo(127);
        assertThat(f.surgeRisk()).isBetween(0.0, 1.0);
        verify(events).countByTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                EventType.PATIENT_ARRIVED, NOW.minus(1, ChronoUnit.HOURS), NOW);
    }

    @Test void stableArrivalsStayStable() {
        history(20, 20, 20, 20);
        ArrivalForecast f = service.forecast(state(20), NOW);
        assertThat(f.trend()).isEqualTo(ArrivalForecast.Trend.STABLE);
        assertThat(f.predictedArrivalsNextHour()).isEqualTo(20);
        assertThat(f.predictedArrivalsNext2Hours()).isEqualTo(40);
        assertThat(f.predictedArrivalsNext4Hours()).isEqualTo(80);
    }

    @Test void fallingArrivalsDoNotBecomeNegative() {
        history(28, 20, 12, 8);
        ArrivalForecast f = service.forecast(state(8), NOW);
        assertThat(f.trend()).isEqualTo(ArrivalForecast.Trend.FALLING);
        assertThat(f.predictedArrivalsNextHour()).isLessThan(12).isNotNegative();
        assertThat(f.predictedArrivalsNext4Hours()).isGreaterThanOrEqualTo(f.predictedArrivalsNext2Hours());
    }

    @Test void insufficientHistoryUsesObservedRate() {
        history(0, 0, 1, 2);
        ArrivalForecast f = service.forecast(state(7.5), NOW);
        assertThat(f.source()).isEqualTo(ArrivalForecast.Source.OBSERVED_RATE);
        assertThat(f.predictedArrivalsNextHour()).isEqualTo(8);
        assertThat(f.predictedArrivalsNext2Hours()).isEqualTo(15);
        assertThat(f.predictedArrivalsNext4Hours()).isEqualTo(30);
    }

    @Test void recentStartupDoesNotTreatMissingHoursAsZeroHistory() {
        history(0, 0, 0, 30);
        when(events.findFirstByTypeOrderByOccurredAtAsc(any())).thenReturn(Optional.of(
                new PatientEvent("new", EventType.PATIENT_ARRIVED, Zone.TRIAGE, Acuity.L3, NOW.minusSeconds(600))));
        assertThat(service.forecast(state(30), NOW).source()).isEqualTo(ArrivalForecast.Source.OBSERVED_RATE);
    }

    @Test void zeroArrivalsAndEmptyDepartmentGiveZeroForecastAndRisk() {
        ArrivalForecast f = service.forecast(state(0), NOW);
        assertThat(f.predictedArrivalsNext4Hours()).isZero();
        assertThat(f.surgeRisk()).isZero();
        assertThat(f.trend()).isEqualTo(ArrivalForecast.Trend.STABLE);
    }

    @Test void unavailableHistoryStillProducesInsights() {
        when(events.countByTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(any(), any(), any()))
                .thenThrow(new DataAccessResourceFailureException("offline"));
        ArrivalForecast f = service.forecast(state(12), NOW);
        assertThat(f.source()).isEqualTo(ArrivalForecast.Source.HISTORY_UNAVAILABLE);
        assertThat(f.predictedArrivalsNext4Hours()).isEqualTo(48);
    }

    @Test void congestionIncreasesRiskAndZeroCapacityIsFinite() {
        ArrivalForecast f = service.forecast(new TwinState(80, 30, 20, 0, 20, 0, 40), NOW);
        assertThat(f.surgeRisk()).isBetween(0.7, 1.0);
        assertThat(f.riskFactors().values()).allMatch(Double::isFinite);
    }
}
