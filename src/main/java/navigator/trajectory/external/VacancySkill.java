package navigator.trajectory.external;

public record VacancySkill(
        int id,
        String name,
        boolean required,
        Integer level,
        Double confidence
) {
}