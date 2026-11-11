package navigator.user;

import java.sql.*;

public class DbUserRepository implements UserRepository {
    private final String dbUrl;
    private final String user;
    private final String password;

    public DbUserRepository(String dbUrl, String user, String password) {
        this.dbUrl = dbUrl;
        this.user = user;
        this.password = password;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, user, password);
    }

    @Override
    public boolean exists(String name) {
        String sql = "SELECT 1 FROM app_users WHERE name = ?";
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось найти пользователя", e);
        }
    }

    @Override
    public User get(String name) {
        String sql = "SELECT id,name FROM app_users WHERE name = ?";
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, name.toLowerCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getLong("id"), rs.getString("name"));
                }
                throw new IllegalStateException("Пользователь не нашелся");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось получить пользователя", e);
        }
    }

    @Override
    public User create(String name) {
        String sql = "INSERT INTO app_users (name) VALUES (?) RETURNING id,name";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name.toLowerCase());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getLong("id"), rs.getString("name"));
                }
                throw new RuntimeException("Не удалось создать пользователя:" + name);
            }
        }catch (SQLException e) {
            throw new RuntimeException("Ошибка при создании пользователя", e);
        }
    }

}
