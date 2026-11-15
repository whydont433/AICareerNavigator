package navigator.db;

import navigator.trajectory.external.RoleCatalog;
import navigator.trajectory.external.RoleInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DbRoleCatalog implements RoleCatalog {
    private final DbConnectionFactory db;

    public DbRoleCatalog(DbConnectionFactory db) {
        this.db = db;
    }

    @Override
    public boolean exists(String roleId) {
        String sql = "SELECT 1 FROM professions WHERE code = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roleId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка проверки роли", e);
        }
    }

    @Override
    public List<RoleInfo> all() {
        String sql = "SELECT code, name FROM professions WHERE code IS NOT NULL ORDER BY id";
        List<RoleInfo> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(new RoleInfo(rs.getString("code"), rs.getString("name")));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка чтения списка ролей", e);
        }
    }
}
