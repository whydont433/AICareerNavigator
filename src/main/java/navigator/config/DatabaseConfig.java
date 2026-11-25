package navigator.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {

    // Параметры подключения можно переопределить переменными окружения
    // DB_URL, DB_USER, DB_PASSWORD. По умолчанию — как в docker-compose.yml.
    private static final String URL =
            env("DB_URL", "jdbc:postgresql://localhost:5432/career_navigator");

    private static final String USER = env("DB_USER", "postgres");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, password());
    }

    public static String url() {
        return URL;
    }

    public static String user() {
        return USER;
    }

    public static String password() {
        return env("DB_PASSWORD", "postgres");
    }

    /**
     * Проверка доступности БД и актуальности схемы при старте приложения.
     * 1) если таблиц ещё нет — создаёт их из schema.sql;
     * 2) гарантирует уникальность skills.canonical_name
     *    (она нужна для INSERT ... ON CONFLICT (canonical_name)).
     */
    public static void ensureSchema() throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            boolean hasTables;
            try (ResultSet rs = statement.executeQuery(
                    "SELECT to_regclass('public.skills') IS NOT NULL")) {
                rs.next();
                hasTables = rs.getBoolean(1);
            }

            if (!hasTables) {
                statement.execute(readSchemaScript());
            }

            // Словарь синонимов навыков (для БД, созданных до этой версии)
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS skill_synonyms (
                        alias TEXT PRIMARY KEY,
                        canonical_name TEXT NOT NULL
                    )
                    """);

            try {
                statement.execute("""
                        CREATE UNIQUE INDEX IF NOT EXISTS skills_canonical_name_uq
                        ON skills (canonical_name)
                        """);
            } catch (SQLException e) {
                // Например, в старой БД уже есть дубли canonical_name
                System.err.println("Предупреждение: не удалось создать уникальный индекс "
                        + "skills.canonical_name (" + e.getMessage() + "). "
                        + "Удалите дубли в таблице skills — иначе анализ вакансий не сможет "
                        + "сохранять навыки.");
            }
        }
    }

    private static String readSchemaScript() {
        try (InputStream in = DatabaseConfig.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("Не найден ресурс schema.sql");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать schema.sql", e);
        }
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
