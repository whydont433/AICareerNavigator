package navigator.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnectionFactory {
    private final String url;
    private final String user;
    private final String password;

    public DbConnectionFactory(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static DbConnectionFactory defaultLocal() {
        return new DbConnectionFactory(
                System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/career_navigator"),
                System.getenv().getOrDefault("DB_USER", "postgres"),
                System.getenv().getOrDefault("DB_PASSWORD", "postgres")
        );
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
