package com.flowtwin.ai;

import com.flowtwin.ai.dto.*;
import com.flowtwin.ai.recommendation.RecommendationEngine;
import com.flowtwin.narration.*;
import com.flowtwin.narration.provider.OpenAiNarrationProvider;
import com.flowtwin.config.LlmProperties;
import com.flowtwin.scenario.*;
import com.flowtwin.service.NarrationService;
import com.flowtwin.twin.TwinState;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class NarrationIntegrationTest {
    private final NarrationProvider provider = mock(NarrationProvider.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final NarrationService service = new NarrationService(provider, new PromptBuilder(), redis);
    private final Metrics baseline = new Metrics(20, 30, 60, 20, 90);
    private final Metrics scenario = new Metrics(10, 15, 30, 10, 80);
    private final ScenarioResult result = new ScenarioResult("Extra nurse", baseline, scenario, scenario.minus(baseline), List.of());

    private AiInsight insight(long arrivals) {
        return new AiInsight(Instant.now(), new TwinState(10, 2, 5, 4, 20, 3, 10),
                new ArrivalForecast(10, arrivals, arrivals * 2, arrivals * 4, ArrivalForecast.Trend.STABLE,
                        0.2, ArrivalForecast.Source.OBSERVED_RATE, 0, Map.of()),
                new BottleneckPrediction(BottleneckPrediction.Zone.NONE, BottleneckPrediction.Severity.LOW,
                        0.4, "Below threshold", Map.of()));
    }

    @Test void redisAndLlmFailuresPreserveFallback() {
        when(redis.opsForValue()).thenThrow(new IllegalStateException("redis offline"));
        when(provider.generate(anyString(), anyString())).thenThrow(new IllegalStateException("offline"));
        assertThat(service.narrate(result).source()).isEqualTo("fallback");
    }

    @Test void missingApiKeyFailsLocallyBeforeHttp() {
        var real = new OpenAiNarrationProvider(new LlmProperties("openai", "", "http://localhost:1", "unused"));
        assertThatThrownBy(() -> real.generate("system", "user"))
                .isInstanceOf(IllegalStateException.class).hasMessage("LLM API key not configured");
    }

    @Test void exactEvidenceChangesCacheKeyButTimestampDoesNot() {
        when(redis.opsForValue()).thenReturn(values);
        when(provider.generate(anyString(), anyString())).thenReturn("Projected waits improve.\n- Review capacity.");
        var recommendation = new RecommendationEngine().rankScenario(result);
        service.narrate(result, insight(10), recommendation);
        service.narrate(result, insight(10), recommendation);
        service.narrate(result, insight(20), recommendation);
        var keys = ArgumentCaptor.forClass(String.class);
        verify(values, times(3)).get(keys.capture());
        assertThat(keys.getAllValues().get(0)).isEqualTo(keys.getAllValues().get(1))
                .isNotEqualTo(keys.getAllValues().get(2));
    }

    @Test void cacheHitAvoidsLlmCall() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn("Cached summary\u0001Review capacity");
        assertThat(service.narrate(result).source()).isEqualTo("ai-cached");
        verifyNoInteractions(provider);
    }

    @Test void cacheWriteFailureKeepsSuccessfulNarration() {
        when(redis.opsForValue()).thenReturn(values);
        when(provider.generate(anyString(), anyString())).thenReturn("Projected waits improve.\n- Review capacity.");
        doThrow(new IllegalStateException("offline")).when(values).set(anyString(), anyString(), any(java.time.Duration.class));
        assertThat(service.narrate(result).source()).isEqualTo("ai");
    }
}
