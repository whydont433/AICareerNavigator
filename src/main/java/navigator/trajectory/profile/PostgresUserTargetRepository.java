package navigator.trajectory.profile;

import navigator.common.Grade;
import navigator.config.DatabaseConfig;
import navigator.trajectory.api.UserTarget;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class PostgresUserTargetRepository implements UserTargetRepository {

    @Override
    public Optional<UserTarget> findTarget(long userId) {
        String sql = """
                SELECT current_profession_id, current_grade
                FROM app_users
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Optional.empty();
                }
                String roleId = rows.getString("current_profession_id");
                String grade = rows.getString("current_grade");
                if (roleId == null || grade == null) {
                    return Optional.empty();
                }
                return Optional.of(new UserTarget(roleId, Grade.valueOf(grade)));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось загрузить целевую роль", e);
        }
    }

    @Override
    public void saveTarget(long userId, UserTarget target) {
        String sql = """
                UPDATE app_users AS u
                SET current_profession_id = p.id, current_grade = ?
                FROM professions AS p
                WHERE u.id = ? AND p.id::text = ?
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, target.grade().name());
            statement.setLong(2, userId);
            statement.setString(3, target.roleId());
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Пользователь или роль не найдены.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось сохранить целевую роль", e);
        }
    }

    @Override
    public int findAvailableTimeWeekly(long userId) {
        String sql = "SELECT available_time_weekly FROM app_users WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getInt("available_time_weekly") : 10;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось загрузить доступное время", e);
        }
    }

    @Override
    public void saveAvailableTimeWeekly(long userId, int hours) {
        String sql = "UPDATE app_users SET available_time_weekly = ? WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, hours);
            statement.setLong(2, userId);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Пользователь не найден.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось сохранить доступное время", e);
        }
    }
}