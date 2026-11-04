package navigator.trajectory.role;

import navigator.common.Grade;
import navigator.trajectory.api.RoleProfile;
import navigator.trajectory.api.RoleProfileApi;
import navigator.trajectory.api.RoleProfileSkill;
import navigator.trajectory.external.RoleCatalog;
import navigator.trajectory.external.RoleInfo;
import navigator.trajectory.external.VacancyData;
import navigator.trajectory.external.VacancyReader;
import navigator.trajectory.external.VacancySkill;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Профили ролей. */
public class RoleProfileService implements RoleProfileApi {
    private static final int PERIOD_DAYS = 30;
    private static final int MIN_VACANCIES = 30;
    private static final double MIN_WEIGHT = 0.03;
    private static final int MAX_SKILLS = 10;

    private final RoleProfileRepository repository;
    private final VacancyReader vacancies;
    private final RoleCatalog roles;

    public RoleProfileService(RoleProfileRepository repository, VacancyReader vacancies, RoleCatalog roles) {
        this.repository = repository;
        this.vacancies = vacancies;
        this.roles = roles;
    }

    @Override
    public Optional<RoleProfile> find(String roleId, Grade grade) {
        return repository.find(roleId, grade);
    }

    @Override
    public List<RoleProfile> getAll() {
        return repository.findAll();
    }


    public int rebuild() {
        List<RoleProfile> built = new ArrayList<>();
        for (RoleInfo role : roles.all()) {
            for (Grade grade : Grade.values()) {
                Optional<RoleProfile> profile = build(role.id(), grade);
                if (profile.isPresent()) {
                    built.add(profile.get());
                }
            }
        }
        repository.replaceAll(built);
        return built.size();
    }

    Optional<RoleProfile> build(String roleId, Grade grade) {
        Instant from = Instant.now().minus(PERIOD_DAYS, ChronoUnit.DAYS);
        List<VacancyData> sample = new ArrayList<>();
        for (VacancyData v : vacancies.find(roleId, grade)) {
            if (v.analyzed() && !v.publishedAt().isBefore(from)) {
                sample.add(v);
            }
        }
        if (sample.size() < MIN_VACANCIES) {
            return Optional.empty();
        }

        Map<Long, Integer> counts = new HashMap<>();
        Map<Long, List<Integer>> levels = new HashMap<>();
        Set<Long> everRequired = new HashSet<>();

        for (VacancyData v : sample) {
            Set<Long> seenInThisVacancy = new HashSet<>();
            for (VacancySkill s : v.skills()) {
                long id = s.skillId();
                if (!seenInThisVacancy.add(id)) {
                    continue;
                }
                counts.merge(id, 1, Integer::sum);
                if (s.required()) {
                    everRequired.add(id);
                }
                if (s.level() != null) {
                    List<Integer> list = levels.computeIfAbsent(id, k -> new ArrayList<>());
                    list.add(s.level());
                    if (s.required()) {
                        list.add(s.level());
                    }
                }
            }
        }

        List<Long> skillIds = new ArrayList<>();
        for (long id : counts.keySet()) {
            if ((double) counts.get(id) / sample.size() >= MIN_WEIGHT) {
                skillIds.add(id);
            }
        }
        skillIds.sort((a, b) -> {
            int byCount = Integer.compare(counts.get(b), counts.get(a));

            return byCount != 0 ? byCount : Long.compare(a, b);
        });
        if (skillIds.size() > MAX_SKILLS) {
            skillIds = skillIds.subList(0, MAX_SKILLS);
        }

        List<RoleProfileSkill> skills = new ArrayList<>();
        for (long id : skillIds) {
            double weight = (double) counts.get(id) / sample.size();
            int required = levels.containsKey(id)
                    ? weightedMedian(levels.get(id))
                    : fallbackLevel(weight, everRequired.contains(id), grade);
            skills.add(new RoleProfileSkill(id, weight, clamp(required, grade)));
        }

        return Optional.of(new RoleProfile(roleId, grade, skills, sample.size(), LocalDateTime.now()));
    }

    static int weightedMedian(List<Integer> values) {
        List<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.get((sorted.size() - 1) / 2);
    }

    static int fallbackLevel(double weight, boolean everRequired, Grade grade) {
        if (!everRequired) {
            return 2;
        }
        return weight >= 0.5 ? baseLevel(grade) : baseLevel(grade) - 1;
    }

    static int clamp(int level, Grade grade) {
        int min = switch (grade) {
            case JUNIOR -> 1;
            case MIDDLE -> 2;
            case SENIOR -> 3;
        };
        return Math.max(min, Math.min(min + 3, level));
    }

    private static int baseLevel(Grade grade) {
        return switch (grade) {
            case JUNIOR -> 3;
            case MIDDLE -> 4;
            case SENIOR -> 5;
        };
    }
}