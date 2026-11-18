package navigator.trajectory.external;

import navigator.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PostgresRoleCatalog implements RoleCatalog {

    @Override
    public boolean exists(String roleId) {
        String sql = "SELECT 1 FROM professions WHERE id::text = ?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, roleId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось проверить роль " + roleId, e);
        }
    }

    @Override
    public List<RoleInfo> all() {
        String sql = "SELECT id, name FROM professions ORDER BY name, id";
        List<RoleInfo> roles = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                roles.add(new RoleInfo(rows.getString("id"), rows.getString("name")));
            }
            return roles;
        } catch (SQLException e) {
            throw new IllegalStateException("Не удалось загрузить список ролей", e);
        }
    }
}