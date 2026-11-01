package navigator.vacancy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.common.HhExperience;


import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;


public class VacancyService {

    private final VacancyRepository repository;

    private final HttpClient client;

    private final ObjectMapper mapper;

    private final String token;

    private final String userAgent;

    private static final String API_URL =
            "https://api.hh.ru";

    public VacancyService(VacancyRepository repository) {

        this.repository = repository;

        this.client = HttpClient.newHttpClient();

        this.mapper = new ObjectMapper();

        this.token = System.getenv("HH_ACCESS_TOKEN");

        this.userAgent = System.getenv().getOrDefault(
                "HH_USER_AGENT",
                "AICareerNavigator/1.0 (drnaleks@gmail.com)"
        );
    }

    // Метод для сбора вакансий
    public int collectVacancies(SearchParams params) throws Exception {

        int collected = 0;

        int page = 0;

        Set<String> processedIds = new HashSet<>();

        while (collected < params.limit() && page < 20) {

            String url = API_URL + "/vacancies"
                    + "?text=" + encode(params.profession())
                    + "&period=30"
                    + "&per_page=100"
                    + "&page=" + page;

            //Получаем список вакансий
            JsonNode response = sendRequest(url);

            JsonNode items = response.get("items");

            if (items == null || items.isEmpty()) {
                break;
            }

            for (JsonNode item : items) {

                if (collected >= params.limit()) {
                    break;
                }

                String hhId = item.path("id").asText("");

                if (hhId.isEmpty() || !processedIds.add(hhId)) {
                    continue;
                }

                //Получаем полную информацию о вакансии
                JsonNode details;

                try {
                    details = sendRequest(
                            API_URL + "/vacancies/" + hhId
                    );
                } catch (Exception e) {
                    continue;
                }

                //Получаем опыт работы
                String experience = details
                        .path("experience")
                        .path("id")
                        .asText("");

                HhExperience hhExperience = HhExperience.fromApi(experience);

                if (hhExperience == null) {
                    continue;
                }

                //Проверяем соответствие уровня
                if (hhExperience.getGrade() != params.grade()) {
                    continue;
                }

                //Проверяем дату публикации
                Instant publishedAt = Instant.parse(
                        details.path("published_at").asText()
                );

                Instant dateLimit = Instant.now()
                        .minus(params.periodDays(), ChronoUnit.DAYS);

                if (publishedAt.isBefore(dateLimit)) {
                    continue;
                }

                //Получаем описание и очищаем HTML
                String html = details
                        .path("description")
                        .asText("");

                String description = HtmlCleaner.clean(html);

                //Получаем название вакансии
                String name = details
                        .path("name")
                        .asText("");

                //Получаем регион
                String area = details
                        .path("area")
                        .path("name")
                        .asText("");

                //Получаем работодателя
                String employer = details
                        .path("employer")
                        .path("name")
                        .asText("");

                //Создаём объект вакансии
                Vacancy vacancy = new Vacancy(
                        null,
                        hhId,
                        name,
                        description,
                        publishedAt,
                        details.path("alternate_url").asText(""),
                        area,
                        employer,
                        hhExperience,
                        hhExperience.getGrade(),
                        Instant.now(),
                        null
                );

                //Сохраняем вакансию
                repository.save(vacancy);

                collected++;

                System.out.println(
                        "Собрана вакансия: " + name
                );
            }

            //Если страниц больше нет, заканчиваем поиск
            int pages = response.path("pages").asInt(0);

            page++;

            if (page >= pages) {
                break;
            }
        }

        return collected;
    }

    //Отправка GET-запроса
    private JsonNode sendRequest(String url) throws Exception {

        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .GET();

        if (token != null && !token.isBlank()) {
            request.header(
                    "Authorization",
                    "Bearer " + token
            );
        }

        HttpResponse<String> response = client.send(
                request.build(),
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "Ошибка HH API: " + response.statusCode()
            );
        }

        return mapper.readTree(response.body());
    }


    private String encode(String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}