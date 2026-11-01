package navigator.trajectory.profile;

import navigator.trajectory.api.UserSkill;

import java.util.List;
import java.util.Optional;

/** Хранилище навыков пользователей **/
public interface UserSkillRepository {

    /** Все навыки пользователя по порядку. **/
    List<UserSkill> findAll(long userId);

    Optional<UserSkill> find(long userId, long skillId);

    /** Добавить или заменить навык **/
    void save(long userId, UserSkill skill);
}