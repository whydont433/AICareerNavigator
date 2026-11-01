package navigator.trajectory.api;

import navigator.common.Grade;

import java.util.Optional;

/** Сравнение пользователя с ролью. **/
public interface MatchingApi {

    /** Пустой - по данной паре роль+грейд нет профиля. */
    Optional<MatchResult> match(long userId, String roleId, Grade grade);
}