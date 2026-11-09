package navigator.trajectory.role;

import navigator.common.Grade;
import navigator.trajectory.api.RoleProfile;

import java.util.List;
import java.util.Optional;

/** Хранилище профилей ролей. */
public interface RoleProfileRepository {

    Optional<RoleProfile> find(String roleId, Grade grade);

    List<RoleProfile> findAll();

    /** Заменяет все профили новыми  **/
    void replaceAll(List<RoleProfile> profiles);
}