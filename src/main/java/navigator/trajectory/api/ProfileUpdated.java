package navigator.trajectory.api;

import java.util.List;

/** Событие у пользователя изменились уровни навыков. */
public record ProfileUpdated(long userId, List<Long> skillIds) {
    public ProfileUpdated {
        skillIds = List.copyOf(skillIds);
    }
}