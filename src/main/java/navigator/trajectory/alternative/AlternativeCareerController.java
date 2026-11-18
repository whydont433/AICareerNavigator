package navigator.trajectory.alternative;

import navigator.Command;
import navigator.common.Grade;
import navigator.trajectory.api.UserSkill;
import navigator.trajectory.external.RoleInfo;
import navigator.trajectory.profile.SkillProfileService;
import navigator.trajectory.role.RoleProfileService;
import navigator.user.UserService;
import java.util.LinkedHashMap;
import java.util.Map;


public class AlternativeCareerController {
    private final UserService users;
    private final SkillProfileService skillProfiles;
    private final RoleProfileService roleProfiles;
    private final AlternativeCareerService alternatives;
    private final navigator.trajectory.external.RoleCatalog roleCatalog;

    public AlternativeCareerController(UserService users, SkillProfileService skillProfiles,
                                       RoleProfileService roleProfiles, AlternativeCareerService alternatives,
                                       navigator.trajectory.external.RoleCatalog roleCatalog) {
        this.users=users; this.skillProfiles=skillProfiles; this.roleProfiles=roleProfiles;
        this.alternatives=alternatives; this.roleCatalog=roleCatalog;
    }
    public Map<String,Command> commands() {
        Map<String,Command> map=new LinkedHashMap<>();
        map.put("roles",this::roles); map.put("role-select",this::selectRole);
        map.put("skill-set",this::setSkill); map.put("my-skills",this::mySkills);
        map.put("rebuild-role-profiles",this::rebuild); map.put("career-alternatives",this::showAlternatives);
        return map;
    }
    private void roles(String[] args) {
        noArgs(args,"roles");
        for(RoleInfo role:skillProfilesRoles()) System.out.println(role.id()+" — "+role.name());
    }
    private java.util.List<RoleInfo> skillProfilesRoles() {
        return roleCatalog.all();
    }
    private void selectRole(String[] args) {
        if(args.length!=2) throw new IllegalArgumentException("Использование: role-select <role-id> <JUNIOR|MIDDLE|SENIOR>");
        long userId=users.requireUser().id(); skillProfiles.selectRole(userId,args[0],Grade.parse(args[1]));
        System.out.println("Целевая профессия сохранена: "+args[0]+" / "+args[1].toUpperCase());
    }
    private void setSkill(String[] args) {
        if(args.length<2) throw new IllegalArgumentException("Использование: skill-set <название навыка> <уровень 0-6>");
        int level; try{level=Integer.parseInt(args[args.length-1]);}catch(NumberFormatException e){throw new IllegalArgumentException("Последний аргумент должен быть уровнем 0-6");}
        String name=String.join(" ",java.util.Arrays.copyOf(args,args.length-1));
        long id=skillProfiles.setLevelByName(users.requireUser().id(),name,level);
        System.out.println("Уровень навыка "+name+" сохранён: "+level+" (skill_id="+id+")");
    }
    private void mySkills(String[] args) {
        noArgs(args,"my-skills"); long id=users.requireUser().id();
        for(UserSkill s:skillProfiles.getSkills(id)) System.out.println("skill_id="+s.skillId()+", level="+s.level()+", source="+s.source());
    }
    private void rebuild(String[] args) {
        noArgs(args,"rebuild-role-profiles"); users.requireUser();
        int count=roleProfiles.rebuild(); System.out.println("Построено профилей профессий: "+count);
    }
    private void showAlternatives(String[] args) {
        if(args.length>1) throw new IllegalArgumentException("Использование: career-alternatives [количество]");
        int limit=5; if(args.length==1) try{limit=Integer.parseInt(args[0]);}catch(NumberFormatException e){throw new IllegalArgumentException("Количество должно быть числом");}
        var routes=alternatives.findRoutes(users.requireUser().id(),limit);
        if(routes.isEmpty()){System.out.println("Альтернативные профессии пока не найдены. Проверьте, построены ли профили профессий.");return;}
        int index=1;
        for(AlternativeCareerRoute route:routes){
            System.out.printf("%d. %s (%s) — %.1f%% соответствия; вакансий в профиле: %d%n",index++,route.profession().name(),route.grade(),route.matchPercent(),route.sampleSize());
            if(!route.strengths().isEmpty()) System.out.println("   Подходящие навыки: "+route.strengths().stream().map(CareerSkill::name).collect(java.util.stream.Collectors.joining(", ")));
            if(!route.gaps().isEmpty()) {
                System.out.println("   Нужно развить:");
                for(CareerSkill s:route.gaps()) System.out.printf("     - %s: ваш уровень %d, требуется %d%s%n",s.name(),s.actualLevel(),s.requiredLevel(),s.blocking()?" (критичный пробел)":"");
            }
        }
    }
    private void noArgs(String[] args,String command){if(args.length!=0)throw new IllegalArgumentException("Использование: "+command);}
}
