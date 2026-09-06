package com.flowtwin.ai.bottleneck;

import com.flowtwin.ai.dto.ArrivalForecast;
import com.flowtwin.ai.dto.BottleneckPrediction;
import com.flowtwin.ai.dto.BottleneckPrediction.Severity;
import com.flowtwin.ai.dto.BottleneckPrediction.Zone;
import com.flowtwin.scenario.Metrics;
import com.flowtwin.twin.TwinState;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class BottleneckPredictionService {
    public BottleneckPrediction predict(TwinState state, ArrivalForecast forecast) {
        return predict(state, forecast, null);
    }

    public BottleneckPrediction predict(TwinState s, ArrivalForecast f, Metrics simulation) {
        double arrivals = f.predictedArrivalsNextHour();
        double queue = Math.max(0, s.triageQueue());
        double occupied = Math.max(0, s.bedsOccupied());
        double load = Math.max(0, s.patientsInDept());
        // Hackathon assumptions: triage takes 6 min (same as simulator), bed stay 90 min,
        // one nurse covers four treatment patients, one doctor covers six patients per hour.
        // Confidence below is evidence strength, not a calibrated probability.
        Map<String, Double> pressure = new LinkedHashMap<>();
        double triage = Math.max(ratio(queue, s.nurses() * 2.0), ratio(queue + arrivals, s.nurses() * 10.0));
        double beds = Math.max(ratio(occupied, s.beds() * 0.8),
                ratio(occupied + Math.max(0, arrivals - occupied / 1.5), s.beds()));
        if (simulation != null) {
            triage = Math.max(triage, ratio(simulation.peakTriageQueue(), s.nurses() * 2.0));
            beds = Math.max(beds, simulation.bedUtilizationPct() / 80.0);
        }
        pressure.put("TRIAGE", triage);
        pressure.put("BEDS", beds);
        pressure.put("DOCTORS", ratio(load + arrivals, s.doctors() * 6.0));
        pressure.put("TREATMENT", ratio(Math.max(0, load - queue), s.nurses() * 4.0));
        String highest = "TRIAGE";
        for (String zone : pressure.keySet()) {
            if (pressure.get(zone) > pressure.get(highest)) highest = zone;
        }
        double peak = pressure.get(highest);
        Zone zone = peak < 0.85 ? Zone.NONE : Zone.valueOf(highest);
        Severity severity = peak >= 2 ? Severity.CRITICAL : peak >= 1.1 ? Severity.HIGH
                : peak >= 0.85 ? Severity.MEDIUM : Severity.LOW;
        String reason = switch (zone) {
            case TRIAGE -> "Current queue and forecast arrivals strain assumed nurse triage capacity.";
            case BEDS -> "Current occupancy or forecast demand strains available bed capacity.";
            case DOCTORS -> "Current patient load and forecast arrivals strain assumed doctor capacity.";
            case TREATMENT -> "Patients beyond triage strain assumed treatment nursing capacity.";
            case NONE -> "No resource exceeds the prototype pressure threshold in the next hour.";
        };
        double confidence = f.source() == ArrivalForecast.Source.HISTORY_BLEND ? 0.65 : 0.40;
        if (simulation != null) confidence += 0.10;
        pressure.replaceAll((k, v) -> Math.round(v * 1000.0) / 1000.0);
        return new BottleneckPrediction(zone, severity, confidence, reason, pressure);
    }

    private static double ratio(double demand, double capacity) {
        // A zero-capacity resource with demand is critical; keep the API finite and JSON-safe.
        return capacity <= 0 ? (demand > 0 ? 10 : 0) : Math.min(10, demand / capacity);
    }
}
