package com.flowtwin.ai;

import com.flowtwin.ai.dto.*;
import com.flowtwin.ai.recommendation.RecommendationEngine;
import com.flowtwin.config.AiProperties;
import com.flowtwin.gemini.GeminiPromptBuilder;
import com.flowtwin.gemini.GeminiService;
import com.flowtwin.gemini.model.ChatResponse;
import com.flowtwin.scenario.*;
import com.flowtwin.service.NarrationService;
import com.flowtwin.twin.TwinState;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NarrationIntegrationTest {
    private final GeminiService gemini = mock(GeminiService.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final NarrationService service = new NarrationService(
            gemini, new GeminiPromptBuilder(), redis, mapper);
    private final Metrics baseline = new Metrics(20, 30, 60, 20, 90);
    private final Metrics scenario = new Metrics(10, 15, 30, 10, 80);
    private final ScenarioResult result = new ScenarioResult(
            "Extra nurse", baseline, scenario, scenario.minus(baseline), List.of());

    private AiInsight insight(long arrivals) {
        return new AiInsight(Instant.now(), new TwinState(10, 2, 5, 4, 20, 3, 10),
                new ArrivalForecast(10, arrivals, arrivals * 2, arrivals * 4,
                        ArrivalForecast.Trend.STABLE, 0.2, ArrivalForecast.Source.OBSERVED_RATE, 0, Map.of()),
                new BottleneckPrediction(BottleneckPrediction.Zone.NONE,
                        BottleneckPrediction.Severity.LOW, 0.4, "Below threshold", Map.of()));
    }

    @Test void redisAndGeminiFailuresPreserveFallback() {
        when(redis.opsForValue()).thenThrow(new IllegalStateException("redis offline"));
        when(gemini.isConfigured()).thenReturn(true);
        when(gemini.generateNarration(any(GeminiPromptBuilder.Prompt.class)))
                .thenThrow(new IllegalStateException("offline"));
        assertThat(service.narrate(result).source()).isEqualTo("fallback");
    }

    @Test void missingApiKeyFailsLocallyBeforeHttp() {
        AiProperties properties = new AiProperties("gemini",
                new AiProperties.Gemini("", "unused", "http://localhost:1", 100));
        GeminiService real = new GeminiService(properties, new GeminiPromptBuilder(), mapper);
        assertThatThrownBy(() -> real.generateNarration(result))
                .isInstanceOf(IllegalStateException.class).hasMessage("GEMINI_API_KEY is not configured");
    }

    @Test void exactEvidenceChangesCacheKeyButTimestampDoesNot() {
        when(redis.opsForValue()).thenReturn(values);
        when(gemini.isConfigured()).thenReturn(true);
        when(gemini.generateNarration(any(GeminiPromptBuilder.Prompt.class)))
                .thenReturn(new ChatResponse("Improves", List.of("Review capacity"), List.of(), "gemini"));
        var recommendation = new RecommendationEngine().rankScenario(result);
        service.narrate(result, insight(10), recommendation);
        service.narrate(result, insight(10), recommendation);
        service.narrate(result, insight(20), recommendation);
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(values, times(3)).get(keys.capture());
        assertThat(keys.getAllValues().get(0)).isEqualTo(keys.getAllValues().get(1))
                .isNotEqualTo(keys.getAllValues().get(2));
    }

    @Test void cacheHitAvoidsGeminiCall() throws Exception {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn(mapper.writeValueAsString(
                new ChatResponse("Cached summary", List.of("Review capacity"), List.of(), "gemini")));
        assertThat(service.narrate(result).source()).isEqualTo("gemini-cached");
        verify(gemini, never()).generateNarration(any(GeminiPromptBuilder.Prompt.class));
    }

    @Test void cacheWriteFailureKeepsSuccessfulNarration() {
        when(redis.opsForValue()).thenReturn(values);
        when(gemini.isConfigured()).thenReturn(true);
        when(gemini.generateNarration(any(GeminiPromptBuilder.Prompt.class)))
                .thenReturn(new ChatResponse("Improves", List.of("Review capacity"), List.of(), "gemini"));
        doThrow(new IllegalStateException("offline")).when(values)
                .set(anyString(), anyString(), any(java.time.Duration.class));
        assertThat(service.narrate(result).source()).isEqualTo("gemini");
    }
}
