package navigator.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final String URL =
            "jdbc:postgresql://localhost:5432/career_navigator";

    private static final String USER = "postgres";

    public static Connection getConnection() throws SQLException {
        String password = System.getenv("DB_PASSWORD");

        return DriverManager.getConnection(URL, USER, password);
    }
}
