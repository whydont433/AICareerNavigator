package navigator.trajectory.external;

import java.time.Instant;
import java.util.List;

/** Вакансия так, как её видит 4.2. analyzed — ИИ успешно разобрал навыки (ai_status). */
public record VacancyData(Instant publishedAt, boolean analyzed, List<VacancySkill> skills) {
    public VacancyData {
        skills = List.copyOf(skills);
    }
}