package navigator.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class HttpLlmClient implements LlmClient {
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public HttpLlmClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.mapper = new ObjectMapper();
        this.apiUrl = System.getenv().getOrDefault("LLM_API_URL", "https://api.openai.com/v1/chat/completions");
        this.apiKey = System.getenv("LLM_API_KEY");
        this.model = System.getenv().getOrDefault("LLM_MODEL", "gpt-4o-mini");
    }

    @Override
    public String completeJson(String promptFileName, String userPayload) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("LLM_API_KEY не задан");
        }
        String systemPrompt = loadPrompt(promptFileName);
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", 0.1,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPayload)
                    )
            );
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new RuntimeException("LLM HTTP " + response.statusCode());
            }
            JsonNode root = mapper.readTree(response.body());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("Пустой ответ LLM");
            }
            return choices.get(0).path("message").path("content").asText();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Ошибка вызова LLM", e);
        }
    }

    private String loadPrompt(String promptFileName) {
        try (InputStream is = getClass().getResourceAsStream("/prompts/" + promptFileName)) {
            if (is == null) {
                return "";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
