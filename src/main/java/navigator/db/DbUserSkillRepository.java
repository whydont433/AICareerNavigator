package navigator.db;

import navigator.trajectory.api.SkillSource;
import navigator.trajectory.api.UserSkill;
import navigator.trajectory.profile.UserSkillRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DbUserSkillRepository implements UserSkillRepository {
    private final DbConnectionFactory db;

    public DbUserSkillRepository(DbConnectionFactory db) {
        this.db = db;
    }

    @Override
    public List<UserSkill> findAll(long userId) {
        String sql = """
                SELECT skill_id, level, source, test_level, test_at, updated_at
                FROM user_skills
                WHERE user_id = ?
                ORDER BY skill_id
                """;
        List<UserSkill> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка загрузки навыков пользователя", e);
        }
    }

    @Override
    public Optional<UserSkill> find(long userId, long skillId) {
        String sql = """
                SELECT skill_id, level, source, test_level, test_at, updated_at
                FROM user_skills
                WHERE user_id = ? AND skill_id = ?
                """;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, skillId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска навыка пользователя", e);
        }
    }

    @Override
    public void save(long userId, UserSkill skill) {
        String sql = """
                INSERT INTO user_skills (user_id, skill_id, level, source, test_level, test_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, skill_id) DO UPDATE SET
                    level = EXCLUDED.level,
                    source = EXCLUDED.source,
                    test_level = EXCLUDED.test_level,
                    test_at = EXCLUDED.test_at,
                    updated_at = EXCLUDED.updated_at
                """;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, skill.skillId());
            stmt.setInt(3, skill.level());
            stmt.setString(4, skill.source().name().toLowerCase());
            if (skill.testLevel() != null) {
                stmt.setInt(5, skill.testLevel());
            } else {
                stmt.setNull(5, Types.INTEGER);
            }
            if (skill.testAt() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(skill.testAt()));
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }
            stmt.setTimestamp(7, Timestamp.valueOf(skill.updatedAt()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения навыка пользователя", e);
        }
    }

    private UserSkill mapRow(ResultSet rs) throws SQLException {
        int rawTestLevel = rs.getInt("test_level");
        Integer testLevel = rs.wasNull() ? null : rawTestLevel;
        Timestamp testTs = rs.getTimestamp("test_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        return new UserSkill(
                rs.getLong("skill_id"),
                rs.getInt("level"),
                SkillSource.valueOf(rs.getString("source").toUpperCase()),
                testLevel,
                testTs != null ? testTs.toLocalDateTime() : null,
                updatedTs != null ? updatedTs.toLocalDateTime() : null
        );
    }
}
