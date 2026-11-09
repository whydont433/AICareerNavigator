package navigator.trajectory.api;

import java.util.List;
import java.util.Optional;

/** Профиль пользователя: его навыки, целевая роль и доступное время. **/
public interface SkillProfileApi {

    /** Все заполненные навыки пользователя. **/
    List<UserSkill> getSkills(long userId);

    /**
     * Установить уровень навыка (0–6).
     * Уровень вне 0–6 - IAE. Публикует событие ProfileUpdated. **/
    void setLevel(long userId, long skillId, int level, SkillSource source);

    /** Целевая роль и грейд. Пустой — пользователь ещё не выбрал роль. **/
    Optional<UserTarget> getTarget(long userId);

    /** Часов в неделю на учёбу. По умолчанию - 10. **/
    int getAvailableTimeWeekly(long userId);

    /** Установить кол-во доступного времени **/
    void setAvailableTimeWeekly(long userId, int hours);
}