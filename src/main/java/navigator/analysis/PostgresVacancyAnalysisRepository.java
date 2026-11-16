package navigator.analysis;

import navigator.config.DatabaseConfig;
import navigator.skills.NormalizedSkill;
import navigator.skills.SkillRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PostgresVacancyAnalysisRepository
        implements VacancyAnalysisRepository {

    private final SkillRepository skillRepository;

    public PostgresVacancyAnalysisRepository(
            SkillRepository skillRepository
    ) {
        this.skillRepository = skillRepository;
    }

    @Override
    public List<VacancyForAnalysis> findUnanalyzedVacancies(
            int limit
    ) throws SQLException {

        String sql = """
                SELECT
                    id,
                    name,
                    description
                FROM vacancies
                WHERE ai_status = FALSE
                  AND description IS NOT NULL
                  AND TRIM(description) <> ''
                ORDER BY id
                LIMIT ?
                """;

        List<VacancyForAnalysis> vacancies = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, limit);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {

                    vacancies.add(
                            new VacancyForAnalysis(
                                    resultSet.getLong("id"),
                                    resultSet.getString("name"),
                                    resultSet.getString("description")
                            )
                    );
                }
            }
        }

        return vacancies;
    }

    @Override
    public void saveVacancySkills(
            Long vacancyId,
            List<NormalizedSkill> skills
    ) throws SQLException {

        String insertSql = """
                INSERT INTO vacancy_skills (
                    vacancy_id,
                    skill_id,
                    need,
                    level,
                    confidence
                )
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (vacancy_id, skill_id)
                DO UPDATE SET
                    need = EXCLUDED.need,
                    level = EXCLUDED.level,
                    confidence = EXCLUDED.confidence
                """;

        String updateStatusSql = """
                UPDATE vacancies
                SET ai_status = TRUE
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection()) {

            connection.setAutoCommit(false);

            try {

                try (PreparedStatement statement =
                             connection.prepareStatement(insertSql)) {

                    for (NormalizedSkill skill : skills) {

                        Long skillId =
                                skillRepository.findOrCreateSkill(
                                        connection,
                                        skill
                                );

                        statement.setLong(1, vacancyId);
                        statement.setLong(2, skillId);
                        statement.setString(3, skill.need());

                        if (skill.level() == null) {
                            statement.setNull(4, Types.INTEGER);
                        } else {
                            statement.setInt(4, skill.level());
                        }

                        statement.setDouble(5, skill.confidence());

                        statement.addBatch();
                    }

                    statement.executeBatch();
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(updateStatusSql)) {

                    statement.setLong(1, vacancyId);

                    int updated = statement.executeUpdate();

                    if (updated != 1) {
                        throw new SQLException(
                                "Вакансия не найдена: " + vacancyId
                        );
                    }
                }

                connection.commit();

            } catch (Exception e) {

                connection.rollback();

                if (e instanceof SQLException sqlException) {
                    throw sqlException;
                }

                throw new SQLException(
                        "Ошибка сохранения навыков вакансии "
                                + vacancyId,
                        e
                );

            } finally {
                connection.setAutoCommit(true);
            }
        }
    }
}
