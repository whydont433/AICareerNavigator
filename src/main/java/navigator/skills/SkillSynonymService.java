package navigator.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.config.DatabaseConfig;
import navigator.gigachat.GigaChatClient;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

//Агрегация синонимов навыков с помощью GigaChat
public class SkillSynonymService {


    private static final int CHUNK_SIZE = 100;

    private final GigaChatClient gigaChatClient;
    private final SkillSynonymDictionary dictionary;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SkillSynonymService(
            GigaChatClient gigaChatClient,
            SkillSynonymDictionary dictionary
    ) {
        this.gigaChatClient = gigaChatClient;
        this.dictionary = dictionary;
    }

    public void loadKnownSynonyms() throws SQLException {

        String sql = "SELECT alias, canonical_name FROM skill_synonym_map";

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                dictionary.learn(rs.getString("alias"), rs.getString("canonical_name"));
            }
        }
    }


    public int aggregate() throws IOException, InterruptedException, SQLException {

        List<String> names = loadCanonicalNames();

        if (names.size() < 2) {
            return 0;
        }


        Map<String, String> found = new LinkedHashMap<>();

        for (int from = 0; from < names.size(); from += CHUNK_SIZE) {

            List<String> chunk =
                    names.subList(from, Math.min(from + CHUNK_SIZE, names.size()));

            String response = gigaChatClient.groupSkillSynonyms(chunk);

            try {
                parseGroups(response, new HashSet<>(chunk), found);
            } catch (IOException | IllegalArgumentException e) {
            }
        }

        int merged = 0;

        for (String alias : found.keySet()) {

            String root = findRoot(found, alias);

            if (root.equals(alias)) {
                continue;
            }

            dictionary.learn(alias, root);

            if (merge(alias, root)) {
                merged++;
            }
        }

        return merged;
    }



    private List<String> loadCanonicalNames() throws SQLException {

        String sql = """
                SELECT canonical_name
                FROM skills
                WHERE canonical_name IS NOT NULL
                ORDER BY canonical_name
                """;

        List<String> names = new ArrayList<>();

        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                names.add(rs.getString("canonical_name"));
            }
        }

        return names;
    }


    private void parseGroups(
            String response,
            Set<String> known,
            Map<String, String> result
    ) throws IOException {

        JsonNode groups = readGroups(extractJson(response));

        if (!groups.isArray()) {
            return;
        }

        for (JsonNode group : groups) {

            String canonical = normalize(group.path("canonical").asText(""));

            if (canonical.isEmpty() || !known.contains(canonical)) {
                continue;
            }

            JsonNode aliases = group.path("aliases");

            if (!aliases.isArray()) {
                continue;
            }

            for (JsonNode node : aliases) {

                String alias = normalize(node.asText(""));

                if (alias.isEmpty()
                        || alias.equals(canonical)
                        || !known.contains(alias)) {
                    continue;
                }

                result.putIfAbsent(alias, canonical);
            }
        }
    }


    private JsonNode readGroups(String json) throws IOException {

        try {
            return objectMapper.readTree(json).path("groups");
        } catch (IOException first) {

            int end = json.lastIndexOf('}');

            while (end > 0) {

                try {
                    return objectMapper
                            .readTree(json.substring(0, end + 1) + "]}")
                            .path("groups");
                } catch (IOException ignored) {
                    end = json.lastIndexOf('}', end - 1);
                }
            }

            throw first;
        }
    }


    private static String findRoot(Map<String, String> map, String alias) {

        Set<String> visited = new HashSet<>();
        String current = alias;

        while (map.containsKey(current) && visited.add(current)) {
            current = map.get(current);
        }

        return current;
    }


    private boolean merge(String alias, String root) throws SQLException {

        try (Connection connection = DatabaseConfig.getConnection()) {

            connection.setAutoCommit(false);

            try {

                saveSynonym(connection, alias, root);

                Long aliasId = findSkillId(connection, alias);
                Long rootId = findSkillId(connection, root);

                boolean merged = aliasId != null
                        && rootId != null
                        && !aliasId.equals(rootId);

                if (merged) {
                    moveReferences(connection, aliasId, rootId);

                    execute(connection,
                            "DELETE FROM skills WHERE id = ?",
                            aliasId);
                }

                connection.commit();

                return merged;

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private void moveReferences(Connection c, long aliasId, long rootId)
            throws SQLException {


        execute(c, """
                UPDATE vacancy_skills t
                SET need = 'required'
                FROM vacancy_skills a
                WHERE a.skill_id = ? AND t.skill_id = ?
                  AND t.vacancy_id = a.vacancy_id
                  AND a.need = 'required'
                """, aliasId, rootId);

        execute(c, """
                UPDATE vacancy_skills
                SET skill_id = ?
                WHERE skill_id = ?
                  AND vacancy_id NOT IN (
                      SELECT vacancy_id FROM vacancy_skills WHERE skill_id = ?)
                """, rootId, aliasId, rootId);

        execute(c, "DELETE FROM vacancy_skills WHERE skill_id = ?", aliasId);


        execute(c, """
                UPDATE user_skills t
                SET level = GREATEST(t.level, a.level)
                FROM user_skills a
                WHERE a.skill_id = ? AND t.skill_id = ?
                  AND t.user_id = a.user_id
                """, aliasId, rootId);

        execute(c, """
                UPDATE user_skills
                SET skill_id = ?
                WHERE skill_id = ?
                  AND user_id NOT IN (
                      SELECT user_id FROM user_skills WHERE skill_id = ?)
                """, rootId, aliasId, rootId);

        execute(c, "DELETE FROM user_skills WHERE skill_id = ?", aliasId);


        execute(c, """
                UPDATE profession_profile_skills
                SET skill_id = ?
                WHERE skill_id = ?
                  AND profile_id NOT IN (
                      SELECT profile_id FROM profession_profile_skills WHERE skill_id = ?)
                """, rootId, aliasId, rootId);

        execute(c, "DELETE FROM profession_profile_skills WHERE skill_id = ?", aliasId);
    }

    private void saveSynonym(Connection c, String alias, String root)
            throws SQLException {

        String sql = """
                INSERT INTO skill_synonym_map (alias, canonical_name)
                VALUES (?, ?)
                ON CONFLICT (alias)
                DO UPDATE SET canonical_name = EXCLUDED.canonical_name
                """;

        try (PreparedStatement statement = c.prepareStatement(sql)) {
            statement.setString(1, alias);
            statement.setString(2, root);
            statement.executeUpdate();
        }
    }

    private Long findSkillId(Connection c, String canonicalName) throws SQLException {

        try (PreparedStatement statement = c.prepareStatement(
                "SELECT id FROM skills WHERE canonical_name = ?")) {

            statement.setString(1, canonicalName);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong("id") : null;
            }
        }
    }

    private static void execute(Connection c, String sql, long... params)
            throws SQLException {

        try (PreparedStatement statement = c.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                statement.setLong(i + 1, params[i]);
            }

            statement.executeUpdate();
        }
    }

    private static String normalize(String text) {

        return text == null
                ? ""
                : text.trim().toLowerCase().replaceAll("\\s+", " ");
    }


    private static String extractJson(String response) {

        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException("GigaChat вернул пустой ответ");
        }

        int start = response.indexOf('{');

        if (start < 0) {
            throw new IllegalArgumentException("GigaChat вернул ответ без JSON");
        }

        return response.substring(start).replace("```", "").trim();
    }
}
