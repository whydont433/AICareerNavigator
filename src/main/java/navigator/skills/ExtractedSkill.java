package navigator.skills;

public record ExtractedSkill(
        String name,
        String category,
        String need,
        Integer level,
        Double confidence
) {
}
