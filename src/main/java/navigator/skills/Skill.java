package navigator.skills;

public record Skill(
        Long id,
        String name,
        String category,
        String canonicalName
) {
}