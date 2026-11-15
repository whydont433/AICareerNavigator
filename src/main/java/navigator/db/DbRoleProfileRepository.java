package navigator.db;

import navigator.common.Grade;
import navigator.trajectory.api.RoleProfile;
import navigator.trajectory.api.RoleProfileSkill;
import navigator.trajectory.role.RoleProfileRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DbRoleProfileRepository implements RoleProfileRepository {
    private final DbConnectionFactory db;

    public DbRoleProfileRepository(DbConnectionFactory db) {
        this.db = db;
    }

    @Override
    public Optional<RoleProfile> find(String roleId, Grade grade) {
        String sql = """
                SELECT pp.id, p.code, pp.grade, pp.sample_size, pp.computed_at
                FROM profession_profiles pp
                JOIN professions p ON p.id = pp.profession_id
                WHERE p.code = ? AND pp.grade = ? AND pp.low_data = FALSE
                """;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roleId);
            stmt.setString(2, grade.name());
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                long profileId = rs.getLong("id");
                int sampleSize = rs.getInt("sample_size");
                LocalDateTime computedAt = rs.getTimestamp("computed_at").toLocalDateTime();
                List<RoleProfileSkill> skills = loadProfileSkills(conn, profileId);
                return Optional.of(new RoleProfile(roleId, grade, skills, sampleSize, computedAt));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка загрузки профиля роли", e);
        }
    }

    @Override
    public List<RoleProfile> findAll() {
        String sql = """
                SELECT pp.id, p.code, pp.grade, pp.sample_size, pp.computed_at
                FROM profession_profiles pp
                JOIN professions p ON p.id = pp.profession_id
                WHERE p.code IS NOT NULL AND pp.low_data = FALSE
                """;
        List<RoleProfile> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                long profileId = rs.getLong("id");
                String code = rs.getString("code");
                Grade grade = Grade.valueOf(rs.getString("grade"));
                int sampleSize = rs.getInt("sample_size");
                LocalDateTime computedAt = rs.getTimestamp("computed_at").toLocalDateTime();
                result.add(new RoleProfile(code, grade, loadProfileSkills(conn, profileId), sampleSize, computedAt));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка загрузки всех профилей ролей", e);
        }
    }

    @Override
    public void replaceAll(List<RoleProfile> profiles) {
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM profession_profiles")) {
                    del.executeUpdate();
                }
                String insertProfile = """
                        INSERT INTO profession_profiles (profession_id, grade, sample_size, computed_at, low_data)
                        VALUES ((SELECT id FROM professions WHERE code = ?), ?, ?, ?, FALSE)
                        RETURNING id
                        """;
                String insertSkill = """
                        INSERT INTO profession_profile_skills (profile_id, skill_id, weight, required_level)
                        VALUES (?, ?, ?, ?)
                        """;
                for (RoleProfile p : profiles) {
                    long profileId;
                    try (PreparedStatement ps = conn.prepareStatement(insertProfile)) {
                        ps.setString(1, p.roleId());
                        ps.setString(2, p.grade().name());
                        ps.setInt(3, p.sampleSize());
                        ps.setTimestamp(4, Timestamp.valueOf(p.computedAt()));
                        try (ResultSet rs = ps.executeQuery()) {
                            rs.next();
                            profileId = rs.getLong(1);
                        }
                    }
                    try (PreparedStatement ss = conn.prepareStatement(insertSkill)) {
                        for (RoleProfileSkill s : p.skills()) {
                            ss.setLong(1, profileId);
                            ss.setLong(2, s.skillId());
                            ss.setDouble(3, s.weight());
                            ss.setInt(4, s.requiredLevel());
                            ss.addBatch();
                        }
                        ss.executeBatch();
                    }
                }
                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения профилей ролей", e);
        }
    }

    private List<RoleProfileSkill> loadProfileSkills(Connection conn, long profileId) throws SQLException {
        String sql = """
                SELECT skill_id, weight, required_level
                FROM profession_profile_skills
                WHERE profile_id = ?
                ORDER BY weight DESC, skill_id ASC
                """;
        List<RoleProfileSkill> list = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, profileId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new RoleProfileSkill(
                            rs.getLong("skill_id"),
                            rs.getDouble("weight"),
                            rs.getInt("required_level")
                    ));
                }
            }
        }
        return list;
    }
}
