package com.flowtwin.gemini;

import com.flowtwin.config.AiProperties;
import com.flowtwin.gemini.model.ChatResponse;
import com.flowtwin.gemini.model.GeminiRequest;
import com.flowtwin.gemini.model.GeminiResponse;
import com.flowtwin.scenario.ScenarioResult;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The single point of contact with the Google Gemini API. Everything Gemini-specific lives here:
 * request construction, the REST call (with the {@code x-goog-api-key} header), and parsing the
 * raw {@link GeminiResponse}. Callers receive only FlowTwin's own {@link ChatResponse}.
 *
 * <p>Gemini NEVER runs the simulation - it only explains and prioritises the numbers FlowTwin's
 * engine already produced, embedded in the grounded prompt by {@link GeminiPromptBuilder}.
 *
 * <p>Flow: {@code ScenarioResult -> GeminiRequest -> Gemini API -> GeminiResponse -> ChatResponse}.
 * Any failure (no key, timeout, HTTP error, blocked content, unparseable JSON) throws, so
 * {@link com.flowtwin.service.NarrationService} can fall back safely.
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final AiProperties.Gemini cfg;
    private final GeminiPromptBuilder promptBuilder;
    private final ObjectMapper mapper;
    private final RestClient client;

    public GeminiService(AiProperties props, GeminiPromptBuilder promptBuilder, ObjectMapper mapper) {
        this.cfg = props.gemini();
        this.promptBuilder = promptBuilder;
        this.mapper = mapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(cfg.timeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(cfg.timeoutMs()));

        this.client = RestClient.builder()
                .baseUrl(cfg.baseUrl())
                .requestFactory(factory)
                .build();
    }

    /** Logs the bound Gemini config at startup so key-binding problems are visible - never the key itself. */
    @PostConstruct
    void logConfig() {
        log.info("Gemini config bound -> API key present: {}, model: {}, baseUrl: {}",
                cfg.hasApiKey(), cfg.model(), cfg.baseUrl());
    }

    /** Whether Gemini is configured and usable right now (an API key is present). */
    public boolean isConfigured() {
        return cfg.hasApiKey();
    }

    /**
     * Generates grounded narration for a simulated scenario.
     *
     * @throws RuntimeException on any transport, timeout, auth, blocked-content, or parse failure.
     */
    public ChatResponse generateNarration(ScenarioResult result) {
        if (!isConfigured()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured");
        }

        GeminiPromptBuilder.Prompt prompt = promptBuilder.build(result);
        GeminiRequest request = GeminiRequest.narration(prompt.system(), prompt.user());

        GeminiResponse response = client.post()
                .uri("/models/{model}:generateContent", cfg.model())
                .header("x-goog-api-key", cfg.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GeminiResponse.class);

        return toChatResponse(response);
    }

    /** Converts the raw Gemini envelope into FlowTwin's {@link ChatResponse} (source = "gemini"). */
    private ChatResponse toChatResponse(GeminiResponse response) {
        if (response == null) {
            throw new IllegalStateException("Empty Gemini response");
        }
        String text = response.firstText();
        if (text.isBlank()) {
            String reason = response.blockReason();
            throw new IllegalStateException(
                    "Gemini returned no usable text" + (reason.isBlank() ? "" : " (blockReason=" + reason + ")"));
        }

        // The candidate text is itself JSON (responseMimeType=application/json + responseSchema).
        JsonNode root = mapper.readTree(stripCodeFence(text.strip()));
        String summary = root.path("summary").asString("").strip();
        List<String> recommendations = collectStrings(root.path("recommendations"));
        List<String> tradeoffs = collectStrings(root.path("tradeoffs"));

        if (summary.isEmpty() && recommendations.isEmpty()) {
            throw new IllegalStateException("Gemini JSON contained neither summary nor recommendations");
        }
        return new ChatResponse(summary, recommendations, tradeoffs, "gemini");
    }

    private static List<String> collectStrings(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode n : node) {
                String s = n.asString("").strip();
                if (!s.isEmpty()) out.add(s);
            }
        }
        return out;
    }

    /** Defensive: strip a ```json ... ``` fence if the model ever wraps its JSON in one. */
    private static String stripCodeFence(String s) {
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl >= 0) s = s.substring(nl + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
        }
        return s.strip();
    }
}
