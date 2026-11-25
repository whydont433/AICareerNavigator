package navigator.skills;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SkillSynonymDictionary {

    private static final Map<String, String> SYNONYMS = Map.ofEntries(

            // JavaScript
            Map.entry("js", "javascript"),
            Map.entry("java script", "javascript"),
            Map.entry("ecmascript", "javascript"),

            // TypeScript
            Map.entry("ts", "typescript"),
            Map.entry("type script", "typescript"),

            // PostgreSQL
            Map.entry("postgres", "postgresql"),
            Map.entry("postgres sql", "postgresql"),

            // Spring Boot
            Map.entry("springboot", "spring boot"),

            // Kubernetes
            Map.entry("k8s", "kubernetes"),

            // C#
            Map.entry("c sharp", "c#"),

            // C++
            Map.entry("cpp", "c++"),

            // HTML
            Map.entry("html5", "html"),

            // CSS
            Map.entry("css3", "css"),

            // Node.js / React / Vue / Go / MongoDB
            Map.entry("nodejs", "node.js"),
            Map.entry("node js", "node.js"),
            Map.entry("node", "node.js"),
            Map.entry("reactjs", "react"),
            Map.entry("react.js", "react"),
            Map.entry("vuejs", "vue"),
            Map.entry("vue.js", "vue"),
            Map.entry("golang", "go"),
            Map.entry("mongo", "mongodb")
    );

    // Синонимы, найденные GigaChat (alias -> canonical).
    // Заполняются из таблицы skill_synonyms при старте и при агрегации навыков.
    private final Map<String, String> learned = new ConcurrentHashMap<>();

    //Запомнить синоним: alias теперь считается тем же навыком, что и canonical
    public void learn(String alias, String canonical) {

        String from = normalize(alias);
        String to = normalize(canonical);

        if (!from.isEmpty() && !to.isEmpty() && !from.equals(to)) {
            learned.put(from, to);
        }
    }

    public String resolve(String skillName) {

        if (skillName == null || skillName.isBlank()) {
            throw new IllegalArgumentException(
                    "Название навыка не может быть пустым"
            );
        }

        String current = normalize(skillName);

        // Идём по цепочке синонимов (js -> javascript -> ...), защищаясь от циклов
        Set<String> visited = new HashSet<>();

        while (visited.add(current)) {

            String next = learned.get(current);

            if (next == null) {
                next = SYNONYMS.get(current);
            }

            if (next == null) {
                break;
            }

            current = next;
        }

        return current;
    }

    private static String normalize(String text) {

        return text == null
                ? ""
                : text.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
