package com.flowtwin.ai.service;

import com.flowtwin.ai.bottleneck.BottleneckPredictionService;
import com.flowtwin.ai.dto.AiInsight;
import com.flowtwin.ai.dto.ArrivalForecast;
import com.flowtwin.ai.forecast.ArrivalForecastService;
import com.flowtwin.scenario.Metrics;
import com.flowtwin.service.TwinStateService;
import com.flowtwin.twin.TwinState;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AiInsightService {
    private final TwinStateService twin;
    private final ArrivalForecastService forecasts;
    private final BottleneckPredictionService bottlenecks;

    public AiInsightService(TwinStateService twin, ArrivalForecastService forecasts,
                            BottleneckPredictionService bottlenecks) {
        this.twin = twin;
        this.forecasts = forecasts;
        this.bottlenecks = bottlenecks;
    }

    public AiInsight insights() { return insights(twin.snapshot(), null); }

    /** Scenario callers reuse their existing snapshot, so narration describes the same state. */
    public AiInsight insights(TwinState state, Metrics baseline) {
        Instant now = Instant.now();
        ArrivalForecast forecast = forecasts.forecast(state, now);
        return new AiInsight(now, state, forecast, bottlenecks.predict(state, forecast, baseline));
    }
}
