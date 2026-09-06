package com.flowtwin.scenario;

import com.flowtwin.simulation.SimResult;

public record Metrics(
        double avgTriageWaitMin,
        double avgBedWaitMin,
        double p90WaitMin,
        int peakTriageQueue,
        double bedUtilizationPct,
        int totalPatients,
        int completedPatients,
        double avgLengthOfStayMin,
        int peakTreatmentQueue
) {
    public Metrics(double avgTriageWaitMin, double avgBedWaitMin, double p90WaitMin,
                   int peakTriageQueue, double bedUtilizationPct) {
        this(avgTriageWaitMin, avgBedWaitMin, p90WaitMin, peakTriageQueue,
                bedUtilizationPct, 0, 0, 0, 0);
    }

    public static Metrics from(SimResult r) {
        return new Metrics(r.avgTriageWaitMin(), r.avgBedWaitMin(), r.p90WaitMin(),
                r.peakTriageQueue(), r.bedUtilizationPct(), r.totalPatients(),
                r.completedPatients(), r.avgLengthOfStayMin(), r.peakTreatmentQueue());
    }

    /** Returns (this - other) per field. Negative wait deltas = improvement. */
    public Metrics minus(Metrics o) {
        return new Metrics(
                round(avgTriageWaitMin - o.avgTriageWaitMin),
                round(avgBedWaitMin - o.avgBedWaitMin),
                round(p90WaitMin - o.p90WaitMin),
                peakTriageQueue - o.peakTriageQueue,
                round(bedUtilizationPct - o.bedUtilizationPct),
                totalPatients - o.totalPatients,
                completedPatients - o.completedPatients,
                round(avgLengthOfStayMin - o.avgLengthOfStayMin),
                peakTreatmentQueue - o.peakTreatmentQueue
        );
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
