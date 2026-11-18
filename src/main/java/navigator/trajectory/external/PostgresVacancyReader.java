package navigator.trajectory.external;

import navigator.common.Grade;
import navigator.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PostgresVacancyReader implements VacancyReader {

    @Override
    public List<VacancyData> find(String roleId, Grade grade) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            List<Integer> vacancyIds = findIds(connection, roleId, grade);
            VacancyDataMapper mapper = new VacancyDataMapper(connection);
            List<VacancyData> vacancies = new ArrayList<>(vacancyIds.size());
            for (int vacancyId : vacancyIds) {
                vacancies.add(mapper.map(vacancyId));
            }
            return vacancies;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Не удалось загрузить вакансии для роли " + roleId + " (" + grade + ")", e
            );
        }
    }

    private List<Integer> findIds(Connection connection, String roleId, Grade grade) throws SQLException {
        String sql = """
                SELECT id
                FROM vacancies
                WHERE profession_id::text = ? AND grade = ?
                ORDER BY id
                """;

        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, roleId);
            statement.setString(2, grade.name());
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    ids.add(rows.getInt("id"));
                }
            }
        }
        return ids;
    }
}