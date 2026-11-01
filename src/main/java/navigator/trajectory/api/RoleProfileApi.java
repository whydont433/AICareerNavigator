package navigator.trajectory.api;

import navigator.common.Grade;

import java.util.List;
import java.util.Optional;

/** Профили ролей **/
public interface RoleProfileApi {

    /** Профиль роль+грейд. Пустой = нет данных. **/
    Optional<RoleProfile> find(String roleId, Grade grade);

    /** Все построенные профили. **/
    List<RoleProfile> getAll();
}