package com.flowtwin.scenario;

import com.flowtwin.simulation.SimResult;

public record Metrics(
        double avgTriageWaitMin,
        double avgBedWaitMin,
        double p90WaitMin,
        int peakTriageQueue,
        double bedUtilizationPct
) {
    public static Metrics from(SimResult r) {
        return new Metrics(r.avgTriageWaitMin(), r.avgBedWaitMin(), r.p90WaitMin(),
                r.peakTriageQueue(), r.bedUtilizationPct());
    }

    /** Returns (this - other) per field. Negative wait deltas = improvement. */
    public Metrics minus(Metrics o) {
        return new Metrics(
                round(avgTriageWaitMin - o.avgTriageWaitMin),
                round(avgBedWaitMin - o.avgBedWaitMin),
                round(p90WaitMin - o.p90WaitMin),
                peakTriageQueue - o.peakTriageQueue,
                round(bedUtilizationPct - o.bedUtilizationPct)
        );
    }

    private static double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
