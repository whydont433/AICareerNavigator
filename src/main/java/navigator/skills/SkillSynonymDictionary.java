package navigator.skills;

import java.util.Map;

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
            Map.entry("css3", "css")
    );

    public String resolve(String skillName) {

        if (skillName == null || skillName.isBlank()) {
            throw new IllegalArgumentException(
                    "Название навыка не может быть пустым"
            );
        }

        String normalized = skillName
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");

        return SYNONYMS.getOrDefault(normalized, normalized);
    }
}
