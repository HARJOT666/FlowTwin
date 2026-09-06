package com.flowtwin.service;

import com.flowtwin.ai.dto.AiInsight;
import com.flowtwin.ai.dto.RecommendationScore;
import com.flowtwin.gemini.GeminiPromptBuilder;
import com.flowtwin.gemini.GeminiService;
import com.flowtwin.gemini.model.ChatResponse;
import com.flowtwin.narration.TemplatedFallback;
import com.flowtwin.scenario.ScenarioResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

/** Cache -> grounded Gemini narration -> deterministic fallback. */
@Service
public class NarrationService {
    private static final Logger log = LoggerFactory.getLogger(NarrationService.class);
    private static final Duration CACHE_TTL = Duration.ofHours(6);
    private final GeminiService gemini;
    private final GeminiPromptBuilder prompts;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public NarrationService(GeminiService gemini, GeminiPromptBuilder prompts,
                            StringRedisTemplate redis, ObjectMapper mapper) {
        this.gemini = gemini;
        this.prompts = prompts;
        this.redis = redis;
        this.mapper = mapper;
    }

    public ChatResponse narrate(ScenarioResult result) {
        return narrate(result, null, null);
    }

    public ChatResponse narrate(ScenarioResult result, AiInsight insight,
                                RecommendationScore recommendation) {
        GeminiPromptBuilder.Prompt prompt = prompts.build(result, insight, recommendation);
        String key = cacheKey(prompt);
        ChatResponse cached = readCache(key);
        if (cached != null) return new ChatResponse(cached.summary(), cached.recommendations(),
                cached.tradeoffs(), "gemini-cached");
        if (!gemini.isConfigured()) {
            log.info("Gemini is not configured; using templated fallback.");
            return TemplatedFallback.build(result);
        }
        try {
            ChatResponse response = gemini.generateNarration(prompt);
            writeCache(key, response);
            return response;
        } catch (Exception ex) {
            log.warn("Gemini narration failed; using templated fallback.");
            return TemplatedFallback.build(result);
        }
    }

    private ChatResponse readCache(String key) {
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? null : mapper.readValue(json, ChatResponse.class);
        } catch (Exception ex) {
            log.debug("Narration cache read skipped.");
            return null;
        }
    }

    private void writeCache(String key, ChatResponse response) {
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(response), CACHE_TTL);
        } catch (Exception ex) {
            log.debug("Narration cache write skipped.");
        }
    }

    private static String cacheKey(GeminiPromptBuilder.Prompt prompt) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((prompt.system() + "\n" + prompt.user()).getBytes(StandardCharsets.UTF_8));
            return "narration:gemini-ai-v2:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
