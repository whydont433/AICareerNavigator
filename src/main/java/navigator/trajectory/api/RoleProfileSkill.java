package navigator.trajectory.api;

/** Навык в профиле роли. weight - доля вакансий с навыком (0–1), requiredLevel (1–6). **/
public record RoleProfileSkill(long skillId, double weight, int requiredLevel) {
}