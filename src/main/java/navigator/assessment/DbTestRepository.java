package navigator.assessment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.db.DbConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DbTestRepository implements TestRepository {
    private final DbConnectionFactory db;
    private final ObjectMapper mapper;

    public DbTestRepository(DbConnectionFactory db) {
        this.db = db;
        this.mapper = new ObjectMapper();
    }

    @Override
    public List<TestQuestion> findUnseenQuestions(long userId, long skillId, int level, int limit) {
        String sql = """
                SELECT q.id, q.skill_id, q.level, q.question, q.options, q.correct_index, q.explanation, q.topic
                FROM test_questions q
                WHERE q.skill_id = ? AND q.level = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM test_answers a
                      JOIN test_attempts att ON att.id = a.attempt_id
                      WHERE att.user_id = ? AND a.question_id = q.id
                  )
                ORDER BY q.id
                LIMIT ?
                """;
        List<TestQuestion> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, skillId);
            stmt.setInt(2, level);
            stmt.setLong(3, userId);
            stmt.setInt(4, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapQuestion(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка выборки вопросов", e);
        }
    }

    @Override
    public List<TestQuestion> findAnyQuestions(long skillId, int level, int limit) {
        String sql = """
                SELECT id, skill_id, level, question, options, correct_index, explanation, topic
                FROM test_questions
                WHERE skill_id = ? AND level = ?
                ORDER BY id
                LIMIT ?
                """;
        List<TestQuestion> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, skillId);
            stmt.setInt(2, level);
            stmt.setInt(3, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapQuestion(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка выборки запасных вопросов", e);
        }
    }

    @Override
    public List<String> findAllQuestionTexts(long skillId, int limit) {
        String sql = "SELECT question FROM test_questions WHERE skill_id = ? ORDER BY id DESC LIMIT ?";
        List<String> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, skillId);
            stmt.setInt(2, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("question"));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка чтения текстов вопросов", e);
        }
    }

    @Override
    public List<TestQuestion> saveAiQuestions(long skillId, List<TestQuestion> questions) {
        String sql = """
                INSERT INTO test_questions (skill_id, level, question, options, correct_index, explanation, topic, source)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'ai')
                RETURNING id
                """;
        List<TestQuestion> saved = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (TestQuestion q : questions) {
                stmt.setLong(1, skillId);
                stmt.setInt(2, q.level());
                stmt.setString(3, q.question());
                stmt.setString(4, mapper.writeValueAsString(q.options()));
                stmt.setInt(5, q.correctIndex());
                stmt.setString(6, q.explanation());
                stmt.setString(7, q.topic());
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    long newId = rs.getLong(1);
                    saved.add(new TestQuestion(newId, skillId, q.level(), q.question(), q.options(),
                            q.correctIndex(), q.explanation(), q.topic()));
                }
            }
            return saved;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сохранения сгенерированных вопросов", e);
        }
    }

    @Override
    public long createAttempt(long userId, long skillId, int targetLevel, int levelFrom, int levelTo) {
        String sql = """
                INSERT INTO test_attempts (user_id, skill_id, target_level, level_from, level_to, status)
                VALUES (?, ?, ?, ?, ?, 'in_progress')
                RETURNING id
                """;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, skillId);
            stmt.setInt(3, targetLevel);
            stmt.setInt(4, levelFrom);
            stmt.setInt(5, levelTo);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка создания попытки теста", e);
        }
    }

    @Override
    public void saveAnswer(long attemptId, long questionId, Integer chosenOption, boolean isCorrect) {
        String sql = """
                INSERT INTO test_answers (attempt_id, question_id, chosen_option, is_correct)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (attempt_id, question_id) DO UPDATE SET
                    chosen_option = EXCLUDED.chosen_option,
                    is_correct = EXCLUDED.is_correct
                """;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, attemptId);
            stmt.setLong(2, questionId);
            if (chosenOption != null) {
                stmt.setInt(3, chosenOption);
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            stmt.setBoolean(4, isCorrect);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка сохранения ответа", e);
        }
    }

    @Override
    public void completeAttempt(long attemptId, int correct, int total, int testLevel) {
        String sql = """
                UPDATE test_attempts
                SET finished_at = NOW(), correct = ?, total = ?, test_level = ?, status = 'completed'
                WHERE id = ?
                """;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, correct);
            stmt.setInt(2, total);
            stmt.setInt(3, testLevel);
            stmt.setLong(4, attemptId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка завершения попытки", e);
        }
    }

    @Override
    public void abandonAttempt(long attemptId) {
        String sql = "UPDATE test_attempts SET finished_at = NOW(), status = 'abandoned' WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, attemptId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка отмены попытки", e);
        }
    }

    @Override
    public Optional<Integer> findLastTestLevel(long userId, long skillId) {
        String sql = """
                SELECT test_level
                FROM test_attempts
                WHERE user_id = ? AND skill_id = ? AND status = 'completed'
                ORDER BY finished_at DESC, id DESC
                LIMIT 1
                """;
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, skillId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("test_level"));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения последнего результата теста", e);
        }
    }

    private TestQuestion mapQuestion(ResultSet rs) throws SQLException {
        try {
            List<String> options = mapper.readValue(rs.getString("options"), new TypeReference<>() {});
            return new TestQuestion(
                    rs.getLong("id"),
                    rs.getLong("skill_id"),
                    rs.getInt("level"),
                    rs.getString("question"),
                    options,
                    rs.getInt("correct_index"),
                    rs.getString("explanation"),
                    rs.getString("topic")
            );
        } catch (Exception e) {
            throw new SQLException("Ошибка разбора вариантов ответа", e);
        }
    }
}
