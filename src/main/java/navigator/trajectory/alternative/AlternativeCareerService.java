package navigator.trajectory.alternative;

import navigator.common.Grade;
import navigator.trajectory.api.*;
import navigator.trajectory.external.RoleCatalog;
import navigator.trajectory.external.RoleInfo;
import navigator.trajectory.external.SkillDictionary;
import navigator.trajectory.matching.MatchingService;
import navigator.trajectory.profile.UserSkillRepository;
import navigator.trajectory.profile.UserTargetRepository;
import navigator.trajectory.role.RoleProfileRepository;
import java.util.*;


public class AlternativeCareerService {
    private final RoleProfileRepository profiles;
    private final UserSkillRepository userSkills;
    private final UserTargetRepository targets;
    private final RoleCatalog roles;
    private final SkillDictionary skills;
    private final MatchingService matching;

    public AlternativeCareerService(RoleProfileRepository profiles, UserSkillRepository userSkills,
                                    UserTargetRepository targets, RoleCatalog roles, SkillDictionary skills) {
        this.profiles=profiles; this.userSkills=userSkills; this.targets=targets;
        this.roles=roles; this.skills=skills; this.matching=new MatchingService(profiles,userSkills);
    }

    public List<AlternativeCareerRoute> findRoutes(long userId,int limit) {
        if(limit<1) throw new IllegalArgumentException("Лимит должен быть не меньше 1");
        UserTarget target=targets.findTarget(userId).orElseThrow(()->new IllegalStateException("Сначала выберите основную профессию: role-select <id> <grade>"));
        List<UserSkill> actual=userSkills.findAll(userId);
        Map<String,RoleInfo> catalog=new HashMap<>();
        for(RoleInfo role:roles.all()) catalog.put(role.id(),role);

        Map<String,AlternativeCareerRoute> best=new HashMap<>();
        for(RoleProfile profile:profiles.findAll()) {
            if(profile.roleId().equals(target.roleId()) || profile.skills().isEmpty()) continue;
            MatchResult result=matching.calculate(profile,actual);
            RoleInfo role=catalog.getOrDefault(profile.roleId(),new RoleInfo(profile.roleId(),profile.roleId()));
            AlternativeCareerRoute candidate=toRoute(role,profile,result);
            AlternativeCareerRoute old=best.get(profile.roleId());
            if(old==null || better(candidate,old)) best.put(profile.roleId(),candidate);
        }
        List<AlternativeCareerRoute> result=new ArrayList<>(best.values());
        result.sort(Comparator.comparingDouble(AlternativeCareerRoute::matchPercent).reversed()
                .thenComparing(Comparator.comparingInt(AlternativeCareerRoute::sampleSize).reversed())
                .thenComparing(r->r.profession().name()));
        return List.copyOf(result.subList(0,Math.min(limit,result.size())));
    }

    private AlternativeCareerRoute toRoute(RoleInfo role,RoleProfile profile,MatchResult result) {
        List<CareerSkill> strengths=result.strengths().stream().map(this::toSkill).toList();
        List<CareerSkill> gaps=result.gaps().stream().map(this::toSkill).toList();
        return new AlternativeCareerRoute(role,profile.grade(),result.matchPercent(),profile.sampleSize(),strengths,gaps);
    }
    private CareerSkill toSkill(SkillMatch s) {
        return new CareerSkill(s.skillId(),skills.nameOf(s.skillId()),s.actualLevel(),s.requiredLevel(),s.weight(),s.priority(),s.blocking());
    }
    private boolean better(AlternativeCareerRoute a,AlternativeCareerRoute b) {
        int score=Double.compare(a.matchPercent(),b.matchPercent()); if(score!=0)return score>0;
        int grade=Integer.compare(rank(a.grade()),rank(b.grade())); if(grade!=0)return grade>0;
        return a.sampleSize()>b.sampleSize();
    }
    private int rank(Grade grade){return switch(grade){case JUNIOR->1;case MIDDLE->2;case SENIOR->3;};}
}
