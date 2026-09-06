package com.flowtwin.ai.controller;

import com.flowtwin.ai.dto.AiInsight;
import com.flowtwin.ai.dto.ArrivalForecast;
import com.flowtwin.ai.dto.BottleneckPrediction;
import com.flowtwin.ai.service.AiInsightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiInsightService insights;

    public AiController(AiInsightService insights) { this.insights = insights; }

    @GetMapping("/insights")
    public AiInsight insights() { return insights.insights(); }

    @GetMapping("/forecast")
    public ArrivalForecast forecast() { return insights.insights().forecast(); }

    @GetMapping("/summary")
    public Summary summary() {
        AiInsight insight = insights.insights();
        return new Summary("Arrival trend: " + insight.forecast().trend() + ". "
                + insight.bottleneck().reason(), insight.forecast().surgeRisk(), insight.bottleneck().zone());
    }

    public record Summary(String summary, double surgeRisk, BottleneckPrediction.Zone recommendedFocus) {}
}
