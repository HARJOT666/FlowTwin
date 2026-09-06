package com.flowtwin.narration.provider;

import com.flowtwin.config.LlmProperties;
import com.flowtwin.narration.NarrationProvider;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * OpenAI-compatible /chat/completions provider. Works with any endpoint speaking that schema
 * (set flowtwin.llm.base-url + model). TODO: dedicated Gemini/Claude providers selected via
 * flowtwin.llm.provider when more than one is on the classpath.
 */
@Component
public class OpenAiNarrationProvider implements NarrationProvider {

    private final LlmProperties props;
    private final RestClient client;

    public OpenAiNarrationProvider(LlmProperties props) {
        this.props = props;
        this.client = RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    @Override
    public String generate(String system, String user) {
        ChatRequest req = new ChatRequest(
                props.model(),
                List.of(new Msg("system", system), new Msg("user", user)),
                0.2);

        ChatResponse resp = client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + props.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(ChatResponse.class);

        if (resp == null || resp.choices() == null || resp.choices().isEmpty()) {
            throw new IllegalStateException("Empty LLM response");
        }
        return resp.choices().get(0).message().content();
    }

    record ChatRequest(String model, List<Msg> messages, double temperature) {}
    record Msg(String role, String content) {}
    record ChatResponse(List<Choice> choices) {}
    record Choice(Msg message) {}
}
