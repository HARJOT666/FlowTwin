package com.flowtwin.simulation;

import java.util.List;

public record SimResult(
        double avgTriageWaitMin,
        double avgBedWaitMin,
        double p90WaitMin,
        int peakTriageQueue,
        double bedUtilizationPct,
        List<TimelinePoint> timeline
) {}
