package com.flowtwin.ai.dto;

import java.util.Map;

public record BottleneckPrediction(Zone zone, Severity severity, double confidence,
                                   String reason, Map<String, Double> pressureRatios) {
    public enum Zone { TRIAGE, TREATMENT, BEDS, DOCTORS, NONE }
    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }
}
