package navigator.trajectory.profile;

import navigator.trajectory.api.UserSkill;

import java.util.*;

public class MemoryUserSkillRepository implements UserSkillRepository {
    private final Map<Long, Map<Long, UserSkill>> skillsByUser = new HashMap<>();

    @Override
    public List<UserSkill> findAll(long userId) {
        Map<Long, UserSkill> skills = skillsByUser.get(userId);
        if (skills == null) {
            return List.of();
        }
        return new ArrayList<>(skills.values());
    }

    @Override
    public Optional<UserSkill> find(long userId, long skillId) {
        Map<Long, UserSkill> skills = skillsByUser.get(userId);
        if (skills == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(skills.get(skillId));
    }

    @Override
    public void save(long userId, UserSkill skill) {
        skillsByUser.computeIfAbsent(userId, id -> new TreeMap<>())
                .put(skill.skillId(), skill);
    }
}
