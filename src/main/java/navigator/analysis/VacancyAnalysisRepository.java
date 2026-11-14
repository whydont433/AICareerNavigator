package navigator.analysis;

import navigator.skills.NormalizedSkill;

import java.sql.SQLException;
import java.util.List;

public interface VacancyAnalysisRepository {

    List<VacancyForAnalysis> findUnanalyzedVacancies(
            int limit
    ) throws SQLException;

    void saveVacancySkills(
            Long vacancyId,
            List<NormalizedSkill> skills
    ) throws SQLException;
}
