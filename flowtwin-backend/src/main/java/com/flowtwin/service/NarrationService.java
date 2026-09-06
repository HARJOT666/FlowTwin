package com.flowtwin.service;

import com.flowtwin.narration.NarrationProvider;
import com.flowtwin.narration.NarrationResult;
import com.flowtwin.narration.PromptBuilder;
import com.flowtwin.narration.TemplatedFallback;
import com.flowtwin.scenario.ScenarioResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class NarrationService {

    private static final Logger log = LoggerFactory.getLogger(NarrationService.class);
    private static final String SEP = "";

    private final NarrationProvider provider;
    private final PromptBuilder prompts;
    private final StringRedisTemplate redis;

    public NarrationService(NarrationProvider provider, PromptBuilder prompts, StringRedisTemplate redis) {
        this.provider = provider;
        this.prompts = prompts;
        this.redis = redis;
    }

    public NarrationResult narrate(ScenarioResult result) {
        String key = "narration:" + Integer.toHexString(result.hashKey());
        String cached = redis.opsForValue().get(key);
        if (cached != null) return decode(cached, "ai-cached");

        try {
            PromptBuilder.Prompt p = prompts.build(result);
            String raw = provider.generate(p.system(), p.user());
            NarrationResult res = parse(raw);
            redis.opsForValue().set(key, encode(res), Duration.ofHours(6));
            return res;
        } catch (Exception ex) {
            log.warn("Narration LLM failed ({}). Falling back to templated summary.", ex.getMessage());
            return TemplatedFallback.build(result);
        }
    }

    private NarrationResult parse(String raw) {
        List<String> recs = new ArrayList<>();
        String summary = "";
        for (String rawLine : raw.split("\\R")) {
            String s = rawLine.strip();
            if (s.isEmpty()) continue;
            if (s.startsWith("-") || s.startsWith("*") || s.startsWith("•")) {
                recs.add(s.replaceFirst("^[-*•]\\s*", ""));
            } else if (summary.isEmpty()) {
                summary = s;
            }
        }
        if (summary.isEmpty()) summary = raw.strip();
        return new NarrationResult(summary, recs, "ai");
    }

    private String encode(NarrationResult r) {
        return r.summary() + SEP + String.join(SEP, r.recommendations());
    }

    private NarrationResult decode(String s, String source) {
        String[] parts = s.split(SEP, -1);
        String summary = parts.length > 0 ? parts[0] : "";
        List<String> recs = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) if (!parts[i].isEmpty()) recs.add(parts[i]);
        return new NarrationResult(summary, recs, source);
    }
}
