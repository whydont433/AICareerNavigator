package navigator.trajectory.external;

/** Навык в вакансии. required: true — обязательный, false — желательный (nice). level — 1–6 или null. */
public record VacancySkill(long skillId, boolean required, Integer level) {
}