package navigator.trajectory.profile;

import navigator.trajectory.api.UserTarget;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MemoryUserTargetRepository implements UserTargetRepository {
    private static final int DEFAULT_HOURS = 10;

    private final Map<Long, UserTarget> targets = new HashMap<>();
    private final Map<Long, Integer> hours = new HashMap<>();

    @Override
    public Optional<UserTarget> findTarget(long userId) {
        return Optional.ofNullable(targets.get(userId));
    }

    @Override
    public void saveTarget(long userId, UserTarget target) {
        targets.put(userId, target);
    }

    @Override
    public int findAvailableTimeWeekly(long userId) {
        return hours.getOrDefault(userId, DEFAULT_HOURS);
    }

    @Override
    public void saveAvailableTimeWeekly(long userId, int value) {
        hours.put(userId, value);
    }
}