package navigator.db;

import navigator.common.Grade;
import navigator.trajectory.api.UserTarget;
import navigator.trajectory.profile.UserTargetRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class DbUserTargetRepository implements UserTargetRepository {
    private final DbConnectionFactory db;

    public DbUserTargetRepository(DbConnectionFactory db) {
        this.db = db;
    }

    @Override
    public Optional<UserTarget> findTarget(long userId) {
        String sql = """
                SELECT p.code, u.current_grade
                FROM app_users u
                LEFT JOIN professions p ON p.id = u.current_profession_id
                WHERE u.id = ?
                """;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String code = rs.getString("code");
                    String grade = rs.getString("current_grade");
                    if (code != null && grade != null) {
                        return Optional.of(new UserTarget(code, Grade.valueOf(grade)));
                    }
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения целевой роли", e);
        }
    }

    @Override
    public void saveTarget(long userId, UserTarget target) {
        String sql = """
                UPDATE app_users
                SET current_profession_id = (SELECT id FROM professions WHERE code = ?),
                    current_grade = ?
                WHERE id = ?
                """;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, target.roleId());
            stmt.setString(2, target.grade().name());
            stmt.setLong(3, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения целевой роли", e);
        }
    }

    @Override
    public int findAvailableTimeWeekly(long userId) {
        String sql = "SELECT available_time_weekly FROM app_users WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("available_time_weekly");
                }
                return 10;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения доступного времени", e);
        }
    }

    @Override
    public void saveAvailableTimeWeekly(long userId, int hours) {
        String sql = "UPDATE app_users SET available_time_weekly = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, hours);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения доступного времени", e);
        }
    }
}
