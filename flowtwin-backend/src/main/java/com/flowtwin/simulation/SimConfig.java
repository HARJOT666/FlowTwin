package com.flowtwin.simulation;

public record SimConfig(
        int horizonMinutes,
        double arrivalRatePerHour,
        double meanTriageMinutes,
        double meanTreatmentMinutes,
        int nurses,
        int beds,
        int doctors,
        long seed
) {}
