package com.flowtwin.ai.dto;

import java.util.Map;

public record ArrivalForecast(
        double currentArrivalRatePerHour,
        long predictedArrivalsNextHour,
        long predictedArrivalsNext2Hours,
        long predictedArrivalsNext4Hours,
        Trend trend,
        double surgeRisk,
        Source source,
        long historicalArrivals,
        Map<String, Double> riskFactors
) {
    public enum Trend { RISING, STABLE, FALLING }
    public enum Source { HISTORY_BLEND, OBSERVED_RATE, HISTORY_UNAVAILABLE }
}
