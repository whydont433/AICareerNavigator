
package navigator.trajectory.external;

import java.sql.*;
import java.util.*;

public class VacancyDataMapper {

    private final Connection connection;

    public VacancyDataMapper(Connection connection) {
        this.connection = connection;
    }


    public VacancyData map(int vacancyId) throws SQLException {

        String sql = """
                SELECT
                    v.id AS vacancy_id,
                    v.published_at,
                    v.ai_status,
                    s.id AS skill_id,
                    s.name AS skill_name,
                    vs.need,
                    vs.level,
                    vs.confidence
                FROM vacancies v
                LEFT JOIN vacancy_skills vs
                    ON v.id = vs.vacancy_id
                LEFT JOIN skills s
                    ON vs.skill_id = s.id
                WHERE v.id = ?
                ORDER BY s.name
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, vacancyId);

            try (ResultSet rs = statement.executeQuery()) {

                List<VacancySkill> skills = new ArrayList<>();

                if (!rs.next()) {
                    throw new SQLException("Вакансия с ID " + vacancyId + " не найдена");
                }

                Timestamp timestamp = rs.getTimestamp("published_at");

                boolean analyzed = rs.getBoolean("ai_status");

                do {
                    VacancySkill skill = extractSkill(rs);

                    if (skill != null) {
                        skills.add(skill);
                    }

                } while (rs.next());

                return new VacancyData(
                        vacancyId,
                        timestamp != null ? timestamp.toInstant() : null,
                        analyzed,
                        skills
                );
            }
        }
    }


    public List<VacancyData> mapAll() throws SQLException {

        String sql = """
                SELECT
                    v.id AS vacancy_id,
                    v.published_at,
                    v.ai_status,
                    s.id AS skill_id,
                    s.name AS skill_name,
                    vs.need,
                    vs.level,
                    vs.confidence
                FROM vacancies v
                LEFT JOIN vacancy_skills vs
                    ON v.id = vs.vacancy_id
                LEFT JOIN skills s
                    ON vs.skill_id = s.id
                ORDER BY v.id, s.name
                """;

        Map<Integer, VacancyBuilder> vacancies = new LinkedHashMap<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {

                int vacancyId = rs.getInt("vacancy_id");

                VacancyBuilder builder = vacancies.computeIfAbsent(
                        vacancyId,
                        id -> new VacancyBuilder(
                                id,
                                getPublishedAt(rs),
                                getAnalyzed(rs)
                        )
                );

                VacancySkill skill = extractSkill(rs);

                if (skill != null) {
                    builder.skills.add(skill);
                }
            }
        }

        List<VacancyData> result = new ArrayList<>();

        for (VacancyBuilder builder : vacancies.values()) {
            result.add(builder.build());
        }

        return result;
    }


    private VacancySkill extractSkill(ResultSet rs)
            throws SQLException {

        String name = rs.getString("skill_name");

        if (name == null) {
            return null;
        }

        String need = rs.getString("need");

        int levelValue = rs.getInt("level");
        Integer level = rs.wasNull() ? null : levelValue;

        double confidenceValue = rs.getDouble("confidence");
        Double confidence = rs.wasNull() ? null : confidenceValue;

        return new VacancySkill(
                rs.getInt("skill_id"),
                name,
                "required".equals(need),
                level,
                confidence
        );
    }

    private static java.time.Instant getPublishedAt(ResultSet rs) {
        try {
            Timestamp timestamp = rs.getTimestamp("published_at");
            return timestamp != null ? timestamp.toInstant() : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean getAnalyzed(ResultSet rs) {
        try {
            return rs.getBoolean("ai_status");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private static class VacancyBuilder {

        private final int vacancyId;
        private final java.time.Instant publishedAt;
        private final boolean analyzed;
        private final List<VacancySkill> skills = new ArrayList<>();

        private VacancyBuilder(
                int vacancyId,
                java.time.Instant publishedAt,
                boolean analyzed
        ) {
            this.vacancyId = vacancyId;
            this.publishedAt = publishedAt;
            this.analyzed = analyzed;
        }

        private VacancyData build() {
            return new VacancyData(
                    vacancyId,
                    publishedAt,
                    analyzed,
                    skills
            );
        }
    }
}
