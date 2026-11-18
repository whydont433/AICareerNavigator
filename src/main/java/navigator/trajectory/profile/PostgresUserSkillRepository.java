package navigator.trajectory.profile;

import navigator.config.DatabaseConfig;
import navigator.trajectory.api.SkillSource;
import navigator.trajectory.api.UserSkill;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class PostgresUserSkillRepository implements UserSkillRepository {

    private static final String SELECT_USER_SKILLS = """
            SELECT skill_id, level, source, test_level, test_at, updated_at
            FROM user_skills
            WHERE user_id = ?
            """;

    @Override
    public List<UserSkill> findAll(long userId) {
        List<UserSkill> skills = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_USER_SKILLS + " ORDER BY skill_id")) {
            statement.setLong(1, userId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    skills.add(readSkill(rows));
                }
            }
            return skills;
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось загрузить навыки пользователя", e);
        }
    }

    @Override
    public Optional<UserSkill> find(long userId, long skillId) {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     SELECT_USER_SKILLS + " AND skill_id = ?")) {
            statement.setLong(1, userId);
            statement.setLong(2, skillId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(readSkill(rows)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось загрузить навык пользователя", e);
        }
    }

    @Override
    public void save(long userId, UserSkill skill) {
        String sql = """
                INSERT INTO user_skills (user_id, skill_id, level, source, test_level, test_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, skill_id)
                DO UPDATE SET level = EXCLUDED.level,
                              source = EXCLUDED.source,
                              test_level = EXCLUDED.test_level,
                              test_at = EXCLUDED.test_at,
                              updated_at = EXCLUDED.updated_at
                """;

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, skill.skillId());
            statement.setInt(3, skill.level());
            statement.setString(4, skill.source().name().toLowerCase(Locale.ROOT));
            statement.setObject(5, skill.testLevel());
            statement.setObject(6, skill.testAt());
            statement.setObject(7, skill.updatedAt());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось сохранить навык пользователя", e);
        }
    }

    private UserSkill readSkill(ResultSet row) throws SQLException {
        return new UserSkill(
                row.getLong("skill_id"),
                row.getInt("level"),
                SkillSource.valueOf(row.getString("source").toUpperCase(Locale.ROOT)),
                row.getObject("test_level", Integer.class),
                row.getObject("test_at", LocalDateTime.class),
                row.getObject("updated_at", LocalDateTime.class)
        );
    }
}