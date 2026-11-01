package navigator.trajectory.api;

import java.time.LocalDateTime;

/**
 * Навык в профиле пользователя.
 * testLevel и testAt — null, если тест по навыку не проходился.
 */
public record UserSkill(
        long skillId,
        int level,                  // 0–6
        SkillSource source,
        Integer testLevel,          // 0–6 или null
        LocalDateTime testAt,       // или null
        LocalDateTime updatedAt
) {
    public boolean tested() {
        return testLevel != null;
    }
}