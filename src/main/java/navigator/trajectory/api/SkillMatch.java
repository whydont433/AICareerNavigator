package navigator.trajectory.api;

/**
 * Один навык роли в результате сравнения.
 * priority = weight · (required − actual)
 * blocking - weight ≥ 0,8 и покрытие < 0,5.
 **/
public record SkillMatch(
        long skillId,
        double weight,
        int requiredLevel,
        int actualLevel,
        double priority,
        boolean blocking
) {
}