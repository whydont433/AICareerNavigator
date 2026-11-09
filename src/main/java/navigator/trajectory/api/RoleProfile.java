package navigator.trajectory.api;

import navigator.common.Grade;

import java.time.LocalDateTime;
import java.util.List;

/** Профиль роли: что нужно роли на конкретном грейде.
 * skills - по убыванию веса, не более 10. **/
public record RoleProfile(
        String roleId,
        Grade grade,
        List<RoleProfileSkill> skills,
        int sampleSize,              // сколько вакансий
        LocalDateTime computedAt     // когда пересчитан
) {
    public RoleProfile {
        skills = List.copyOf(skills);
    }
}