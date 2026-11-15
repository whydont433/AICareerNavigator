package navigator.db;

import navigator.trajectory.external.SkillDictionary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DbSkillDictionary implements SkillDictionary {
    private final DbConnectionFactory db;

    public DbSkillDictionary(DbConnectionFactory db) {
        this.db = db;
    }

    @Override
    public List<Long> find(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String query = text.trim().toLowerCase();
        String sql = """
                SELECT DISTINCT s.id
                FROM skills s
                LEFT JOIN skill_synonyms ss ON s.id = ss.skill_id
                WHERE LOWER(s.name) = ? OR LOWER(s.canonical_name) = ? OR LOWER(ss.synonym) = ?
                ORDER BY s.id
                """;
        List<Long> ids = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, query);
            stmt.setString(2, query);
            stmt.setString(3, query);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong("id"));
                }
            }
            return ids;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска навыка", e);
        }
    }

    @Override
    public String nameOf(long skillId) {
        String sql = "SELECT COALESCE(canonical_name, name) AS title FROM skills WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, skillId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("title");
                }
                return "навык #" + skillId;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения названия навыка", e);
        }
    }

    public Map<Long, String> findAllMap() {
        String sql = "SELECT id, COALESCE(canonical_name, name) AS title FROM skills ORDER BY id";
        Map<Long, String> result = new LinkedHashMap<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getLong("id"), rs.getString("title"));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка загрузки справочника навыков", e);
        }
    }
}
