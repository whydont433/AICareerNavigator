package navigator.trajectory.matching;

import navigator.common.Grade;
import navigator.trajectory.api.*;
import navigator.trajectory.profile.UserSkillRepository;
import navigator.trajectory.role.RoleProfileRepository;

import java.util.*;

public class MatchingService implements MatchingApi {
    private static final double BLOCKING_WEIGHT = 0.8;
    private static final double BLOCKING_COVERAGE = 0.5;

    private final RoleProfileRepository roleProfiles;
    private final UserSkillRepository userSkills;

    public MatchingService(RoleProfileRepository roleProfiles, UserSkillRepository userSkills) {
        this.roleProfiles = roleProfiles;
        this.userSkills = userSkills;
    }


    @Override
    public Optional<MatchResult> match(long userId, String roleId, Grade grade) {
        Optional<RoleProfile> profile = roleProfiles.find(roleId, grade);
        if (profile.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(calculate(profile.get(), userSkills.findAll(userId)));
    }

    public MatchResult calculate(RoleProfile profile, List<UserSkill> skills) {
        Map<Long, Integer> levels = new HashMap<>();

        for (UserSkill s : skills) {
            levels.put(s.skillId(), s.level());
        }


        List<SkillMatch> strengths = new ArrayList<>();
        List<SkillMatch> gaps = new ArrayList<>();
        List<Long> unfilled = new ArrayList<>();
        Set<Long> roleSkillIds = new HashSet<>();
        double weightSum = 0;
        double coveredSum = 0;

        for (RoleProfileSkill roleProfileSkill : profile.skills()) {
            long roleProfileSkillId = roleProfileSkill.skillId();
            double roleProfileSkillWeight = roleProfileSkill.weight();
            int roleProfileSkillRequiredLevel = roleProfileSkill.requiredLevel();

            roleSkillIds.add(roleProfileSkillId);
            boolean skillFilled = levels.containsKey(roleProfileSkillId);
            int userSkillLevel = skillFilled ? levels.get(roleProfileSkillId) : 0;
            if (!skillFilled) {
                unfilled.add(roleProfileSkillId);
            }
            double coverage = Math.min((double) userSkillLevel / roleProfileSkillRequiredLevel, 1.0);
            weightSum += roleProfileSkillWeight;
            coveredSum += roleProfileSkillWeight * coverage;

            if (userSkillLevel >= roleProfileSkillRequiredLevel) {
                strengths.add(new SkillMatch(roleProfileSkillId, roleProfileSkillWeight, roleProfileSkillRequiredLevel,
                        userSkillLevel, 0, false));

            } else {
                double priority = roleProfileSkillWeight * (roleProfileSkillRequiredLevel - userSkillLevel);
                boolean blocking = roleProfileSkillWeight >= BLOCKING_WEIGHT && coverage < BLOCKING_COVERAGE;
                gaps.add(new SkillMatch(roleProfileSkillId, roleProfileSkillWeight, roleProfileSkillRequiredLevel,
                        userSkillLevel, priority, blocking));
            }
        }
        gaps.sort(Comparator.comparingDouble(SkillMatch::priority).reversed()
                .thenComparing(Comparator.comparingDouble(SkillMatch::weight).reversed())
                .thenComparingLong(SkillMatch::skillId));

        List<SkillMatch> blockingGaps = new ArrayList<>();
        for (SkillMatch g : gaps) {
            if (g.blocking()) {
                blockingGaps.add(g);
            }
        }

        List<UserSkill> extra = new ArrayList<>();
        for (UserSkill s : skills) {
            if (!roleSkillIds.contains(s.skillId())) {
                extra.add(s);
            }
        }

        double percent = weightSum == 0 ? 0 : coveredSum / weightSum * 100;   // пустой профиль — 0%

        return new MatchResult(profile.roleId(), profile.grade(), percent,
                strengths, gaps, blockingGaps, unfilled, extra);
    }
}
