package com.flowtwin.ai.forecast;

import com.flowtwin.ai.dto.ArrivalForecast;
import com.flowtwin.ai.dto.ArrivalForecast.Source;
import com.flowtwin.ai.dto.ArrivalForecast.Trend;
import com.flowtwin.model.EventType;
import com.flowtwin.repository.PatientEventRepository;
import com.flowtwin.twin.TwinState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class ArrivalForecastService {
    private static final Logger log = LoggerFactory.getLogger(ArrivalForecastService.class);
    private final PatientEventRepository events;

    public ArrivalForecastService(PatientEventRepository events) {
        this.events = events;
    }

    public ArrivalForecast forecast(TwinState state, Instant now) {
        double observed = nonNegative(state.observedArrivalRatePerHour());
        double baseline = observed, rate = observed, slope = 0;
        long total = 0;
        Source source = Source.OBSERVED_RATE;
        try {
            Instant from = now.minus(4, ChronoUnit.HOURS);
            // Half-open windows avoid double counting at hourly boundaries and exclude future events.
            long[] hours = new long[4];
            for (int i = 0; i < hours.length; i++) {
                hours[i] = events.countByTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                        EventType.PATIENT_ARRIVED, from.plus(i, ChronoUnit.HOURS),
                        from.plus(i + 1, ChronoUnit.HOURS));
                total += hours[i];
            }
            boolean coverage = events.findFirstByTypeOrderByOccurredAtAsc(EventType.PATIENT_ARRIVED)
                    .map(e -> !e.getOccurredAt().isAfter(from)).orElse(false);
            // Sparse history cannot establish a trustworthy trend. Never fill missing hours randomly.
            if (coverage && total >= 8) {
                double ewma = hours[0];
                for (int i = 1; i < hours.length; i++) ewma = 0.5 * hours[i] + 0.5 * ewma;
                baseline = total / 4.0;
                double recent = (hours[2] + hours[3]) / 2.0;
                double earlier = (hours[0] + hours[1]) / 2.0;
                slope = Math.max(-baseline * 0.25, Math.min(baseline * 0.25, (recent - earlier) / 2));
                rate = 0.5 * ewma + 0.3 * recent + 0.2 * observed;
                source = Source.HISTORY_BLEND;
            }
        } catch (DataAccessException ex) {
            log.warn("Arrival history unavailable; using the current observed arrival rate.");
            total = 0;
            source = Source.HISTORY_UNAVAILABLE;
        }

        Trend trend = slope > Math.max(0.5, baseline * 0.05) ? Trend.RISING
                : slope < -Math.max(0.5, baseline * 0.05) ? Trend.FALLING : Trend.STABLE;
        double cumulative = 0;
        long one = 0, two = 0, four = 0;
        for (int h = 1; h <= 4; h++) {
            // Damped extrapolation keeps a short-lived trend from exploding at the four-hour horizon.
            rate = Math.max(0, rate + slope * Math.pow(0.75, h - 1));
            cumulative += rate;
            if (h == 1) one = Math.round(cumulative);
            if (h == 2) two = Math.round(cumulative);
            if (h == 4) four = Math.round(cumulative);
        }
        double growth = clamp((one - baseline) / Math.max(1, baseline));
        double queue = clamp(Math.max(0, state.triageQueue()) / Math.max(1.0, state.nurses() * 2.0));
        double occupancy = clamp(Math.max(0, state.bedsOccupied()) / Math.max(1.0, state.beds()));
        double demand = clamp(one / Math.max(1.0, state.nurses() * 10.0));
        // Operational heuristic, not an estimated probability of a clinical outcome.
        double risk = round(0.30 * growth + 0.25 * queue + 0.25 * occupancy + 0.20 * demand);
        return new ArrivalForecast(observed, one, two, four, trend, risk, source, total,
                Map.of("arrivalGrowth", round(growth), "queuePressure", round(queue),
                        "bedOccupancy", round(occupancy), "triageDemand", round(demand)));
    }

    private static double nonNegative(double value) { return Double.isFinite(value) ? Math.max(0, value) : 0; }
    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
    private static double round(double value) { return Math.round(value * 1000.0) / 1000.0; }
}
