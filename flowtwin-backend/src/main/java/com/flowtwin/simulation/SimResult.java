package com.flowtwin.simulation;

import java.util.List;

/** Simulation output. avgBedWaitMin is the wait for both a doctor and a bed. */
public record SimResult(
        double avgTriageWaitMin,
        double avgBedWaitMin,
        double p90WaitMin,
        int peakTriageQueue,
        double bedUtilizationPct,
        int totalPatients,
        int completedPatients,
        double avgLengthOfStayMin,
        int peakTreatmentQueue,
        List<TimelinePoint> timeline
) {
    /** Preserves source compatibility with the original result shape. */
    public SimResult(double avgTriageWaitMin, double avgBedWaitMin, double p90WaitMin,
                     int peakTriageQueue, double bedUtilizationPct, List<TimelinePoint> timeline) {
        this(avgTriageWaitMin, avgBedWaitMin, p90WaitMin, peakTriageQueue,
                bedUtilizationPct, 0, 0, 0, 0, timeline);
    }
}
