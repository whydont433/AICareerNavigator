package navigator.trajectory.temporary;

import navigator.trajectory.external.SkillDictionary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Заглушка 4.1: справочник навыков с синонимами. */
public class StubSkillDictionary implements SkillDictionary {
    private final Map<Long, String> names = new LinkedHashMap<>();
    private final Map<Long, List<String>> aliases = new LinkedHashMap<>();

    public StubSkillDictionary() {
        add(1, "Java", "java", "джава");
        add(2, "SQL", "sql", "базы данных");
        add(3, "Spring", "spring", "spring boot", "спринг");
        add(4, "Docker", "docker", "докер");
        add(5, "Git", "git", "гит");
        add(6, "PostgreSQL", "postgresql", "postgres", "постгрес", "базы данных");
        add(7, "Kafka", "kafka", "кафка");
        add(8, "Python", "python", "питон");
        add(9, "Django", "django", "джанго");
        add(10, "Linux", "linux", "линукс");
        add(11, "REST API", "rest", "rest api");
        add(12, "Kotlin", "kotlin", "котлин");
    }

    private void add(long id, String name, String... synonyms) {
        names.put(id, name);
        aliases.put(id, List.of(synonyms));
    }

    @Override
    public List<Long> find(String text) {
        String query = text == null ? "" : text.trim().toLowerCase();
        List<Long> result = new ArrayList<>();
        for (long id : names.keySet()) {
            if (names.get(id).equalsIgnoreCase(query) || aliases.get(id).contains(query)) {
                result.add(id);
            }
        }
        return result;
    }

    @Override
    public String nameOf(long skillId) {
        return names.getOrDefault(skillId, "навык #" + skillId);
    }
}