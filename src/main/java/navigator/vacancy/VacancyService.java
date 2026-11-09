package navigator.vacancy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.vacancy.HtmlCleaner;
import navigator.vacancy.SearchParams;
import navigator.vacancy.Vacancy;
import navigator.vacancy.VacancyRepository;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class VacancyService {

    private static final String API_URL =
            "https://api.superjob.ru/2.0";

    private final VacancyRepository repository;

    private final HttpClient client;

    private final ObjectMapper mapper;

    private final String apiKey;

    public VacancyService(VacancyRepository repository) {

        this.repository = repository;

        this.client = HttpClient.newHttpClient();

        this.mapper = new ObjectMapper();

        this.apiKey = System.getenv("SUPERJOB_API_KEY");


    }

    // Основной метод сбора вакансий
    public int collectVacancies(SearchParams params)
            throws IOException, InterruptedException {

        int collected = 0;

        Set<String> processedIds = new HashSet<>();

        // Максимальное количество вакансий
        int limit = SearchParams.VACANCY_LIMIT;



        // Получаем категории опыта для выбранного уровня
        var experiences =
                org.example.vacancy.SuperJobExperience.forGrade(params.grade());

        for (org.example.vacancy.SuperJobExperience experience : experiences) {

            if (collected >= limit) {
                break;
            }

            int page = 0;
            boolean more = true;

            while (more && collected < limit) {

                int count = Math.min(100, limit - collected);

                String url = buildSearchUrl(
                        params.profession(),
                        experience,
                        count,
                        page
                );

                JsonNode response = sendRequest(url);

                JsonNode objects = response.path("objects");

                if (!objects.isArray() || objects.isEmpty()) {
                    break;
                }

                for (JsonNode item : objects) {

                    if (collected >= limit) {
                        break;
                    }

                    String sourceId = item.path("id").asText("");

                    if (sourceId.isBlank()
                            || !processedIds.add(sourceId)) {
                        continue;
                    }

                    Vacancy vacancy = parseVacancy(item);

                    if (vacancy == null) {
                        continue;
                    }

                    repository.save(vacancy);

                    collected++;

                    System.out.println(
                            "Найдена вакансия: " + vacancy.name()
                    );
                }

                more = response.path("more").asBoolean(false);

                page++;
            }
        }

        return collected;
    }

    // Формирование URL запроса
    private String buildSearchUrl(
            String profession,
            org.example.vacancy.SuperJobExperience experience,
            int count,
            int page
    ) {

        return API_URL + "/vacancies/"
                + "?keyword=" + encode(profession)
                + "&experience=" + experience.getApiValue()
                + "&order_field=date"
                + "&order_direction=desc"
                + "&count=" + count
                + "&page=" + page;
    }

    // Отправка GET-запроса к SuperJob API
    private JsonNode sendRequest(String url)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Api-App-Id", apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {

            throw new RuntimeException(
                    "Ошибка SuperJob API. HTTP "
                            + response.statusCode()
                            + ": " + response.body()
            );
        }

        return mapper.readTree(response.body());
    }

    // Преобразование JSON-объекта в Vacancy
    private Vacancy parseVacancy(JsonNode item) {

        String sourceId = item.path("id").asText("");

        if (sourceId.isBlank()) {
            return null;
        }

        String name = item.path("profession").asText("");

        if (name.isBlank()) {
            return null;
        }


        String work = item.path("work").asText("");

        // Требования к кандидату
        String candidat = item.path("candidat").asText("");


        String compensation = item.path("compensation").asText("");


        String description = """
                Обязанности:
                %s

                Требования:
                %s

                Условия:
                %s
                """.formatted(work, candidat, compensation);

        description = HtmlCleaner.clean(description);


        long publishedTimestamp =
                item.path("date_published").asLong(0);

        Instant publishedAt = publishedTimestamp > 0
                ? Instant.ofEpochSecond(publishedTimestamp)
                : null;


        String experienceId =
                item.path("experience").path("id").asText("");

        org.example.vacancy.SuperJobExperience experience =
                org.example.vacancy.SuperJobExperience.fromApi(experienceId);

        if (experience == null) {
            return null;
        }


        String area = item.path("town").path("title").asText("");

        String employer = item.path("firm_name").asText("");

        String url = item.path("link").asText("");

        return new Vacancy(
                null,
                sourceId,
                name,
                description,
                publishedAt,
                url,
                area,
                employer,
                experience,
                experience.getGrade(),
                Instant.now(),
                null
        );
    }

    // Кодирование параметров URL
    private String encode(String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}