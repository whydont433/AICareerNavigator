package navigator.gigachat;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

public class GigaChatClient {

    private static final String AUTH_URL =
            "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";

    private static final String CHAT_URL =
            "https://gigachat.devices.sberbank.ru/api/v1/chat/completions";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private final String authKey;
    private final String scope;
    private final String model;

    private String accessToken;
    private Instant tokenExpiresAt;

    public GigaChatClient() {

        this.authKey = requireEnv("GIGACHAT_AUTH_KEY");

        this.scope = getEnv(
                "GIGACHAT_SCOPE",
                "GIGACHAT_API_PERS"
        );

        this.model = getEnv(
                "GIGACHAT_MODEL",
                "GigaChat"
        );

        this.httpClient = HttpClient.newHttpClient();

        this.objectMapper = new ObjectMapper();
    }

    private static String requireEnv(String name) {

        String value = System.getenv(name);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Не задана переменная окружения " + name
            );
        }

        return value;
    }

    private static String getEnv(
            String name,
            String defaultValue
    ) {

        String value = System.getenv(name);

        return value == null || value.isBlank()
                ? defaultValue
                : value;
    }

    private synchronized String getAccessToken()
            throws IOException, InterruptedException {

        if (accessToken != null
                && tokenExpiresAt != null
                && Instant.now().isBefore(tokenExpiresAt)) {

            return accessToken;
        }

        String form = "scope=" + URLEncoder.encode(
                scope,
                StandardCharsets.UTF_8
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AUTH_URL))
                .header("Content-Type",
                        "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .header("RqUID", UUID.randomUUID().toString())
                .header("Authorization", "Basic " + authKey)
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Ошибка авторизации GigaChat: HTTP "
                            + response.statusCode()
                            + ", ответ: " + response.body()
            );
        }

        JsonNode json = objectMapper.readTree(response.body());

        accessToken = json.path("access_token").asText(null);

        long expiresAt = json.path("expires_at").asLong(0);

        if (accessToken == null || expiresAt == 0) {
            throw new IOException(
                    "Некорректный ответ OAuth GigaChat"
            );
        }

        tokenExpiresAt = Instant
                .ofEpochMilli(expiresAt)
                .minusSeconds(60);

        return accessToken;
    }

    public String extractSkills(String description)
            throws IOException, InterruptedException {

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException(
                    "Описание вакансии пустое"
            );
        }

        String token = getAccessToken();

        ObjectNode body = objectMapper.createObjectNode();

        body.put("model", model);
        body.put("temperature", 0.1);

        ArrayNode messages = body.putArray("messages");

        ObjectNode systemMessage = messages.addObject();

        systemMessage.put("role", "system");

        systemMessage.put("content", """
                Ты — эксперт по анализу вакансий в IT и извлечению профессиональных навыков.
                
                        Твоя задача — анализировать описание вакансии и извлекать
                        только наиболее важные HARD SKILLS.
                
                        HARD SKILLS — это конкретные профессиональные знания,
                        технологии, инструменты и технические компетенции,
                        необходимые для выполнения рабочих задач.
                
                        1. ИЗВЛЕКАЙ ТОЛЬКО HARD SKILLS.
                
                        Разрешённые категории:
                        - programming_language — языки программирования;
                        - framework — фреймворки и библиотеки;
                        - database — базы данных и технологии хранения данных;
                        - devops — DevOps, CI/CD, контейнеризация, облачные технологии;
                        - tool — инструменты разработки и администрирования;
                        - testing — тестирование и автоматизация тестирования;
                        - architecture — архитектура и технические подходы;
                        - operating_system — операционные системы;
                        - other — другие конкретные технические навыки.
                
                        НЕ ИЗВЛЕКАЙ:
                        - коммуникабельность;
                        - ответственность;
                        - стрессоустойчивость;
                        - умение работать в команде;
                        - лидерские качества;
                        - обучаемость;
                        - инициативность;
                        - тайм-менеджмент;
                        - аналитическое мышление;
                        - другие личностные качества и soft skills.
                
                        2. ВЫБИРАЙ НЕ БОЛЕЕ 10 НАВЫКОВ.
                
                        - Выбирай максимум 10 наиболее важных и востребованных
                          технических навыков для данной вакансии.
                        - Сначала выбирай навыки, которые прямо указаны
                          в требованиях вакансии.
                        - Учитывай также технические навыки, которые указаны
                          в обязанностях, если они действительно важны.
                        - Не добавляй общие и малозначимые навыки.
                        - Не дублируй один навык под разными названиями.
                        - Не придумывай технологии, которые не упоминаются
                          и не следуют непосредственно из описания вакансии.
                
                        3. ОПРЕДЕЛЯЙ НЕОБХОДИМОСТЬ НАВЫКА.
                
                        need:
                        - required — обязательный навык;
                        - nice — дополнительное преимущество.
                
                        4. ОЦЕНИВАЙ УРОВЕНЬ НАВЫКА.
                
                        Используй шкалу от 1 до 6:
                        1 — базовое знакомство;
                        2 — начальные знания;
                        3 — уверенное применение;
                        4 — продвинутый уровень;
                        5 — глубокая экспертиза;
                        6 — экспертный уровень.
                
                        Не завышай уровень, если описание не содержит
                        достаточной информации. Если уровень определить
                        невозможно, возвращай null.
                
                        5. ОЦЕНИВАЙ УВЕРЕННОСТЬ.
                
                        confidence — число от 0 до 1, отражающее уверенность
                        в том, что навык действительно требуется вакансии.
                
                        6. ФОРМАТ ОТВЕТА.
                
                        Возвращай исключительно корректный JSON без Markdown,
                        пояснений и дополнительного текста.
                
                        Строго соблюдай структуру:
                
                        {
                          "skills": [
                            {
                              "name": "Java",
                              "category": "programming_language",
                              "need": "required",
                              "level": 4,
                              "confidence": 0.98
                            }
                          ]
                        }
                
                        Если hard skills отсутствуют, верни:
                
                        {
                          "skills": []
                        }
                
                        В массиве skills должно быть не более 10 элементов.
                """);

        ObjectNode userMessage = messages.addObject();

        userMessage.put("role", "user");

        userMessage.put(
                "content",
                "Проанализируй вакансию и извлеки навыки:\n\n"
                        + description
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(CHAT_URL))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(body)
                ))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Ошибка GigaChat API: HTTP "
                            + response.statusCode()
                            + ", ответ: " + response.body()
            );
        }

        JsonNode json = objectMapper.readTree(response.body());

        JsonNode content = json
                .path("choices")
                .path(0)
                .path("message")
                .path("content");

        if (content.isMissingNode() || content.isNull()) {
            throw new IOException(
                    "GigaChat не вернул содержимое ответа"
            );
        }

        return content.asText();
    }
}