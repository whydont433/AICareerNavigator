package navigator.trajectory.role;

import navigator.common.Grade;
import navigator.config.DatabaseConfig;
import navigator.trajectory.api.RoleProfile;
import navigator.trajectory.api.RoleProfileSkill;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostgresRoleProfileRepository implements RoleProfileRepository {

    private static final String SELECT_PROFILES = """
            SELECT p.id, p.profession_id, p.grade, p.sample_size, p.computed_at,
                   s.skill_id, s.weight, s.required_level
            FROM profession_profiles AS p
            LEFT JOIN profession_profile_skills AS s ON s.profile_id = p.id
            WHERE NOT p.low_data
            """;

    private static final String PROFILE_ORDER = """
            ORDER BY p.profession_id, p.grade, s.weight DESC, s.skill_id
            """;

    @Override
    public Optional<RoleProfile> find(String roleId, Grade grade) {
        String sql = SELECT_PROFILES + " AND p.profession_id::text = ? AND p.grade = ? " + PROFILE_ORDER;
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, roleId);
            statement.setString(2, grade.name());
            return readProfiles(statement).stream().findFirst();
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось загрузить профиль роли", e);
        }
    }

    @Override
    public List<RoleProfile> findAll() {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PROFILES + PROFILE_ORDER)) {
            return readProfiles(statement);
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось загрузить профили ролей", e);
        }
    }

    @Override
    public void replaceAll(List<RoleProfile> profiles) {
        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Сериализуем замену, сохраняя доступ читателей к предыдущему набору до commit.
                try (Statement statement = connection.createStatement()) {
                    statement.execute("LOCK TABLE profession_profiles IN SHARE ROW EXCLUSIVE MODE");
                    statement.executeUpdate("DELETE FROM profession_profiles");
                }
                insertProfiles(connection, profiles);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось обновить профили ролей", e);
        }
    }

    private List<RoleProfile> readProfiles(PreparedStatement statement) throws SQLException {
        List<ProfileRow> profiles = new ArrayList<>();
        try (ResultSet rows = statement.executeQuery()) {
            ProfileRow profile = null;
            while (rows.next()) {
                long profileId = rows.getLong("id");
                if (profile == null || profile.id() != profileId) {
                    profile = new ProfileRow(
                            profileId,
                            rows.getString("profession_id"),
                            Grade.valueOf(rows.getString("grade")),
                            rows.getInt("sample_size"),
                            rows.getObject("computed_at", LocalDateTime.class),
                            new ArrayList<>()
                    );
                    profiles.add(profile);
                }
                long skillId = rows.getLong("skill_id");
                if (!rows.wasNull()) {
                    profile.skills().add(new RoleProfileSkill(
                            skillId, rows.getDouble("weight"), rows.getInt("required_level")
                    ));
                }
            }
        }

        return profiles.stream()
                .map(row -> new RoleProfile(
                        row.roleId(), row.grade(), row.skills(), row.sampleSize(), row.computedAt()
                ))
                .toList();
    }

    private void insertProfiles(Connection connection, List<RoleProfile> profiles) throws SQLException {
        String profileSql = """
                INSERT INTO profession_profiles (profession_id, grade, sample_size, computed_at)
                SELECT id, ?, ?, ? FROM professions WHERE id::text = ?
                RETURNING id
                """;
        String skillSql = """
                INSERT INTO profession_profile_skills (profile_id, skill_id, weight, required_level)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement profileInsert = connection.prepareStatement(profileSql);
             PreparedStatement skillInsert = connection.prepareStatement(skillSql)) {
            for (RoleProfile profile : profiles) {
                profileInsert.setString(1, profile.grade().name());
                profileInsert.setInt(2, profile.sampleSize());
                profileInsert.setObject(3, profile.computedAt());
                profileInsert.setString(4, profile.roleId());

                long profileId;
                try (ResultSet rows = profileInsert.executeQuery()) {
                    if (!rows.next()) {
                        throw new SQLException("Роль не найдена: " + profile.roleId());
                    }
                    profileId = rows.getLong("id");
                }

                for (RoleProfileSkill skill : profile.skills()) {
                    skillInsert.setLong(1, profileId);
                    skillInsert.setLong(2, skill.skillId());
                    skillInsert.setDouble(3, skill.weight());
                    skillInsert.setInt(4, skill.requiredLevel());
                    skillInsert.addBatch();
                }
            }
            skillInsert.executeBatch();
        }
    }

    private record ProfileRow(
            long id,
            String roleId,
            Grade grade,
            int sampleSize,
            LocalDateTime computedAt,
            List<RoleProfileSkill> skills
    ) {
    }
}