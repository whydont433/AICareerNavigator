package navigator.trajectory.api;

import navigator.common.Grade;

import java.util.List;

/** Результат сравнения с профилем роли. **/
public record MatchResult(
        String roleId,
        Grade grade,
        double matchPercent,
        List<SkillMatch> strengths,     // actual >= required
        List<SkillMatch> gaps,          // actual < required, по убыванию приоритета
        List<SkillMatch> blockingGaps,  // блокирующие
        List<Long> unfilledSkillIds,    // навыки роли, которые пользователь не заполнил
        List<UserSkill> extraSkills     // есть у пользователя, но нет в профиле роли; в процент не входят
) {
    public MatchResult {
        strengths = List.copyOf(strengths);
        gaps = List.copyOf(gaps);
        blockingGaps = List.copyOf(blockingGaps);
        unfilledSkillIds = List.copyOf(unfilledSkillIds);
        extraSkills = List.copyOf(extraSkills);
    }
}