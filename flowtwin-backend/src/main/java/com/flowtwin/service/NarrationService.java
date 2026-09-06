package com.flowtwin.service;

import com.flowtwin.gemini.GeminiService;
import com.flowtwin.gemini.model.ChatResponse;
import com.flowtwin.narration.TemplatedFallback;
import com.flowtwin.scenario.ScenarioResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * Turns a simulated {@link ScenarioResult} into a {@link ChatResponse}.
 *
 * <p>Responsibilities are deliberately narrow: check the cache, invoke {@link GeminiService}, and
 * fall back to {@link TemplatedFallback} when Gemini is not configured or fails. All Gemini HTTP,
 * request building and response parsing live in {@link GeminiService} - not here.
 *
 * <p>Flow: cache -> Gemini ({@code "gemini"}) -> deterministic fallback ({@code "fallback"}).
 */
@Service
public class NarrationService {

    private static final Logger log = LoggerFactory.getLogger(NarrationService.class);
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    private final GeminiService gemini;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public NarrationService(GeminiService gemini, StringRedisTemplate redis, ObjectMapper mapper) {
        this.gemini = gemini;
        this.redis = redis;
        this.mapper = mapper;
    }

    public ChatResponse narrate(ScenarioResult result) {
        String key = "narration:" + Integer.toHexString(result.hashKey());

        ChatResponse cached = readCache(key);
        if (cached != null) return cached;

        if (!gemini.isConfigured()) {
            log.info("Gemini is not configured; using templated fallback.");
            return TemplatedFallback.build(result);
        }

        try {
            ChatResponse res = gemini.generateNarration(result);
            writeCache(key, res);
            return res;
        } catch (Exception ex) {
            log.warn("Gemini narration failed ({}). Falling back to templated summary.", ex.getMessage());
            return TemplatedFallback.build(result);
        }
    }

    private ChatResponse readCache(String key) {
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? null : mapper.readValue(json, ChatResponse.class);
        } catch (Exception ex) {
            log.debug("Narration cache read skipped ({}).", ex.getMessage());
            return null;
        }
    }

    private void writeCache(String key, ChatResponse res) {
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(res), CACHE_TTL);
        } catch (Exception ex) {
            log.debug("Narration cache write skipped ({}).", ex.getMessage());
        }
    }
}
