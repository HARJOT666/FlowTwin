package com.flowtwin.ai.dto;

import com.flowtwin.twin.TwinState;
import java.time.Instant;

public record AiInsight(Instant timestamp, TwinState currentState,
                        ArrivalForecast forecast, BottleneckPrediction bottleneck) {}
