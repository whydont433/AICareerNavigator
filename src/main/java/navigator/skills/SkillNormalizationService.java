package navigator.skills;

import java.util.Locale;
import java.util.Set;

public class SkillNormalizationService {

    private static final Set<String> ALLOWED_NEEDS =
            Set.of("required", "nice");

    private static final Set<String> ALLOWED_CATEGORIES =
            Set.of(
                    "programming_language",
                    "framework",
                    "database",
                    "devops",
                    "tool",
                    "testing",
                    "architecture",
                    "operating_system",
                    "other"
            );

    private final SkillSynonymDictionary synonymDictionary;

    public SkillNormalizationService(
            SkillSynonymDictionary synonymDictionary
    ) {
        this.synonymDictionary = synonymDictionary;
    }

    public NormalizedSkill normalize(ExtractedSkill skill) {

        if (skill == null || skill.name() == null
                || skill.name().isBlank()) {
            throw new IllegalArgumentException(
                    "Некорректный навык"
            );
        }

        String canonicalName =
                synonymDictionary.resolve(skill.name());

        String category = normalizeCategory(skill.category());

        String need = normalizeNeed(skill.need());

        Integer level = skill.level();

        if (level != null && (level < 1 || level > 6)) {
            throw new IllegalArgumentException(
                    "Уровень навыка должен быть от 1 до 6"
            );
        }

        Double confidence = skill.confidence();

        if (confidence == null
                || confidence < 0
                || confidence > 1) {
            throw new IllegalArgumentException(
                    "Некорректная confidence"
            );
        }

        return new NormalizedSkill(
                skill.name().trim(),
                canonicalName,
                category,
                need,
                level,
                confidence
        );
    }

    private String normalizeNeed(String need) {

        if (need == null) {
            throw new IllegalArgumentException(
                    "Не указано требование к навыку"
            );
        }

        String normalized = need
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!ALLOWED_NEEDS.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Неизвестный тип требования: " + need
            );
        }

        return normalized;
    }

    private String normalizeCategory(String category) {

        if (category == null) {
            return "other";
        }

        String normalized = category
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!ALLOWED_CATEGORIES.contains(normalized)) {
            return "other";
        }

        return normalized;
    }
}