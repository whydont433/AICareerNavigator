package navigator.trajectory.profile;

import navigator.trajectory.api.UserTarget;

import java.util.Optional;


public interface UserTargetRepository {

    /** Пусто, если пользователь ещё не выбрал роль. */
    Optional<UserTarget> findTarget(long userId);

    void saveTarget(long userId, UserTarget target);

    /**DEFAULT 10  **/
    int findAvailableTimeWeekly(long userId);

    void saveAvailableTimeWeekly(long userId, int hours);
}