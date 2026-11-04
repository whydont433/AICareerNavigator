package navigator.trajectory.temporary;

import navigator.common.Grade;
import navigator.trajectory.external.VacancyData;
import navigator.trajectory.external.VacancyReader;
import navigator.trajectory.external.VacancySkill;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Фейковые вакансии.
 * Java Middle — 40 вакансий: 4 не разобраны ИИ, 4 старше 30 дней → в расчёт идут 32.
 * Python Middle — 10 вакансий → меньше 30, профиль не строится.
 */
public class StubVacancyReader implements VacancyReader {
    private final List<VacancyData> javaMiddle = new ArrayList<>();
    private final List<VacancyData> pythonMiddle = new ArrayList<>();

    public StubVacancyReader() {
        Instant now = Instant.now();

        for (int i = 0; i < 40; i++) {
            Instant published = i >= 36 ? now.minus(40, ChronoUnit.DAYS) : now.minus(i % 25, ChronoUnit.DAYS);
            boolean analyzed = i < 32 || i >= 36;

            List<VacancySkill> skills = new ArrayList<>();
            skills.add(new VacancySkill(1, true, i % 7 == 0 ? 5 : 4));               // Java — во всех
            if (i % 5 != 0) skills.add(new VacancySkill(3, true, 4));                // Spring — 80%
            if (i % 3 != 0) skills.add(new VacancySkill(2, true, 3));                // SQL — ~2/3
            if (i % 3 == 0) skills.add(new VacancySkill(4, false, null));            // Docker — треть, nice, без уровня
            if (i % 2 == 0) skills.add(new VacancySkill(5, false, 2));               // Git — половина, nice
            if (i % 4 == 0) skills.add(new VacancySkill(6, true, 3));                // PostgreSQL — четверть
            if (i % 10 == 0) skills.add(new VacancySkill(7, false, 3));              // Kafka — редко
            if (i == 13) skills.add(new VacancySkill(12, false, 2));                 // Kotlin — 1 раз, отсечётся по 3%

            javaMiddle.add(new VacancyData(published, analyzed, skills));
        }

        for (int i = 0; i < 10; i++) {
            pythonMiddle.add(new VacancyData(now.minus(i, ChronoUnit.DAYS), true, List.of(
                    new VacancySkill(8, true, 4),
                    new VacancySkill(9, i % 2 == 0, 3))));
        }
    }

    @Override
    public List<VacancyData> find(String roleId, Grade grade) {
        if (grade != Grade.MIDDLE) {
            return List.of();
        }
        if (roleId.equals("java-dev")) {
            return javaMiddle;
        }
        if (roleId.equals("python-dev")) {
            return pythonMiddle;
        }
        return List.of();
    }
}