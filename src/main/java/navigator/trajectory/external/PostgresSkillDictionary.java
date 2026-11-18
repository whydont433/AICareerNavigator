package navigator.trajectory.external;

import navigator.config.DatabaseConfig;
import navigator.skills.SkillSynonymDictionary;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PostgresSkillDictionary implements SkillDictionary {

    private final SkillSynonymDictionary synonyms;

    public PostgresSkillDictionary(SkillSynonymDictionary synonyms) {
        this.synonyms = synonyms;
    }

    @Override
    public List<Long> find(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String name = text.strip().toLowerCase(Locale.ROOT);
        String canonicalName = synonyms.resolve(text);
        String sql = """
                SELECT id
                FROM skills
                WHERE LOWER(name) IN (?, ?) OR LOWER(canonical_name) = ?
                ORDER BY id
                """;

        List<Long> ids = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            statement.setString(2, canonicalName);
            statement.setString(3, canonicalName);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    ids.add(rows.getLong("id"));
                }
            }
            return ids;
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось найти навык " + text, e);
        }
    }

    @Override
    public String nameOf(long skillId) {
        String sql = "SELECT name FROM skills WHERE id = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, skillId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getString("name") : "навык #" + skillId;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось загрузить название навыка " + skillId, e);
        }
    }
}