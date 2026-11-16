package navigator.skills;

public record NormalizedSkill(
        String name,
        String canonicalName,
        String category,
        String need,
        Integer level,
        Double confidence
) {
}