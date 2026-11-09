package navigator.trajectory.profile;

import navigator.common.Grade;
import navigator.trajectory.api.ProfileUpdated;
import navigator.trajectory.api.RoleProfile;
import navigator.trajectory.api.RoleProfileSkill;
import navigator.trajectory.api.SkillProfileApi;
import navigator.trajectory.api.SkillSource;
import navigator.trajectory.api.UserSkill;
import navigator.trajectory.api.UserTarget;
import navigator.trajectory.external.RoleCatalog;
import navigator.trajectory.external.SkillDictionary;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Профиль пользователя. Уровни навыков, целевая роль, часы в неделю. */
public class SkillProfileService implements SkillProfileApi {
    private static final int MIN_LEVEL = 0;
    private static final int MAX_LEVEL = 6;

    private final UserSkillRepository skills;
    private final UserTargetRepository targets;
    private final RoleCatalog roles;
    private final SkillDictionary dictionary;
    private final List<Consumer<ProfileUpdated>> listeners = new ArrayList<>();

    public SkillProfileService(UserSkillRepository skills, UserTargetRepository targets,
                               RoleCatalog roles, SkillDictionary dictionary) {
        this.skills = skills;
        this.targets = targets;
        this.roles = roles;
        this.dictionary = dictionary;
    }

    /** Подписаться на ProfileUpdated. */
    public void onProfileUpdated(Consumer<ProfileUpdated> listener) {
        listeners.add(listener);
    }

    @Override
    public List<UserSkill> getSkills(long userId) {
        return skills.findAll(userId);
    }

    @Override
    public void setLevel(long userId, long skillId, int level, SkillSource source) {
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("Уровень от 0 до 6");
        }
        if (source == null) {
            throw new IllegalArgumentException("Не указан источник уровня");
        }

        Optional<UserSkill> old = skills.find(userId, skillId);
        Integer testLevel = old.isPresent() ? old.get().testLevel() : null;
        LocalDateTime testAt = old.isPresent() ? old.get().testAt() : null;

        skills.save(userId, new UserSkill(skillId, level, source, testLevel, testAt, LocalDateTime.now()));

        boolean changed = old.isEmpty() || old.get().level() != level;
        if (changed) {
            publish(new ProfileUpdated(userId, List.of(skillId)));
        }
    }

    /** Команда skill set <название> <уровень>. Возвращает id навыка. */
    public long setLevelByName(long userId, String skillText, int level) {
        List<Long> found = dictionary.find(skillText);
        if (found.isEmpty()) {
            throw new IllegalArgumentException("Навык «" + skillText + "» не найден.");
        }
        if (found.size() > 1) {
            List<String> names = new ArrayList<>();
            for (long id : found) {
                names.add(dictionary.nameOf(id));
            }
            throw new IllegalArgumentException(
                    "«" + skillText + "» — это несколько навыков: " + String.join(", ", names));
        }
        long skillId = found.get(0);
        setLevel(userId, skillId, level, SkillSource.MANUAL);
        return skillId;
    }

    @Override
    public Optional<UserTarget> getTarget(long userId) {
        return targets.findTarget(userId);
    }

    /** Команда role select.  */
    public void selectRole(long userId, String roleId, Grade grade) {
        if (!roles.exists(roleId)) {
            throw new IllegalArgumentException("Роль «" + roleId + "» не найдена. Список: roles");
        }
        targets.saveTarget(userId, new UserTarget(roleId, grade));
    }

    @Override
    public int getAvailableTimeWeekly(long userId) {
        return targets.findAvailableTimeWeekly(userId);
    }

    @Override
    public void setAvailableTimeWeekly(long userId, int hours) {
        if (hours < 0 || hours > 168) {
            throw new IllegalArgumentException("Часов в неделю: от 0 до 168");
        }
        targets.saveAvailableTimeWeekly(userId, hours);
    }

    public List<RoleProfileSkill> unfilledSkills(long userId, RoleProfile profile) {
        List<RoleProfileSkill> result = new ArrayList<>();
        for (RoleProfileSkill rs : profile.skills()) {
            if (skills.find(userId, rs.skillId()).isEmpty()) {
                result.add(rs);
            }
        }
        return result;
    }

    public Optional<Integer> recordTest(long userId, long skillId, int testLevel) {
        if (testLevel < MIN_LEVEL || testLevel > MAX_LEVEL) {
            throw new IllegalArgumentException("Уровень по тесту от 0 до 6");
        }
        LocalDateTime now = LocalDateTime.now();
        Optional<UserSkill> old = skills.find(userId, skillId);

        if (old.isEmpty()) {
            skills.save(userId, new UserSkill(skillId, testLevel, SkillSource.TEST, testLevel, now, now));
            publish(new ProfileUpdated(userId, List.of(skillId)));
            return Optional.empty();
        }

        UserSkill s = old.get();
        skills.save(userId, new UserSkill(skillId, s.level(), s.source(), testLevel, now, s.updatedAt()));
        if (s.level() == testLevel) {
            return Optional.empty();
        }
        return Optional.of(s.level());
    }

    private void publish(ProfileUpdated event) {
        for (Consumer<ProfileUpdated> l : listeners) {
            l.accept(event);
        }
    }
}