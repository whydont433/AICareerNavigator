package navigator.trajectory.external;

import java.util.List;

/** Список ролей из roles.yaml реализация - заглушка. */
public interface RoleCatalog {

    boolean exists(String roleId);

    /** Все роли — для команды roles. */
    List<RoleInfo> all();
}