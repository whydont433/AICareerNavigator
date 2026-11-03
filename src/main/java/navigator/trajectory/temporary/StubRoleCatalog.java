package navigator.trajectory.temporary;

import navigator.trajectory.external.RoleCatalog;
import navigator.trajectory.external.RoleInfo;

import java.util.List;

/** Заглушка 4.1: пара ролей вручную. */
public class StubRoleCatalog implements RoleCatalog {
    private final List<RoleInfo> roles = List.of(
            new RoleInfo("java-dev", "Java Developer"),
            new RoleInfo("python-dev", "Python Developer")
    );

    @Override
    public boolean exists(String roleId) {
        for (RoleInfo r : roles) {
            if (r.id().equals(roleId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<RoleInfo> all() {
        return roles;
    }
}