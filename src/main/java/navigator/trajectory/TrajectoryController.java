package navigator.trajectory;

import navigator.Command;
import navigator.common.Grade;
import navigator.trajectory.api.MatchResult;
import navigator.trajectory.matching.MatchConsoleView;
import navigator.trajectory.matching.MatchingService;
import navigator.trajectory.api.RoleProfile;
import navigator.trajectory.api.RoleProfileSkill;
import navigator.trajectory.api.SkillSource;
import navigator.trajectory.api.UserSkill;
import navigator.trajectory.api.UserTarget;
import navigator.trajectory.external.RoleCatalog;
import navigator.trajectory.external.RoleInfo;
import navigator.trajectory.external.SkillDictionary;
import navigator.trajectory.profile.SkillProfileService;
import navigator.trajectory.role.RoleProfileService;
import navigator.user.UserService;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class TrajectoryController {
    private static final String LEVEL_HINT =
            "0 нет · 1 слышал · 2 изучал · 3 пет-проект · 4 с помощью · 5 сам · 6 эксперт";

    private static final String LEVEL_SCALE = """
            0 — не знаком
            1 — читал, понимаю назначение, почти не использовал
            2 — решал учебные задачи, изучал документацию
            3 — делал пет-проект
            4 — применял в реальном проекте с подсказками
            5 — уверенно применяю в работе
            6 — проектирую и оптимизирую
            """;

    private final UserService users;
    private final RoleCatalog roles;
    private final RoleProfileService roleProfiles;
    private final SkillProfileService skillProfiles;
    MatchingService matching;
    private final SkillDictionary dictionary;
    private final Scanner scanner;
    private final MatchConsoleView matchView;

    public TrajectoryController(
            UserService users,
            RoleCatalog roles,
            RoleProfileService roleProfiles,
            SkillProfileService skillProfiles,
            MatchingService matching,
            SkillDictionary dictionary,
            Scanner scanner
    ) {
        this.users = users;
        this.roles = roles;
        this.roleProfiles = roleProfiles;
        this.skillProfiles = skillProfiles;
        this.matching = matching;
        this.dictionary = dictionary;
        this.scanner = scanner;
        this.matchView = new MatchConsoleView(roles, dictionary);
    }

    public Map<String, Command> commands() {
        return Map.of(
                "roles", this::roles,
                "role", this::role,
                "fill", this::fill,
                "skill", this::skill,
                "profile", this::profile,
                "match", this::match
        );
    }

    private void roles(String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("rebuild")) {
            System.out.println("Профилей рассчитано: " + roleProfiles.rebuild());
            return;
        }
        if (args.length != 0) {
            throw new IllegalArgumentException("Использование: roles или roles rebuild");
        }
        for (RoleInfo role : roles.all()) {
            System.out.println(role.id() + " — " + role.name());
        }
    }

    private void role(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Использование: role select или role show");
        }

        String[] targetArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "select" -> selectRole(targetArgs);
            case "show" -> showRole(targetOrCurrent(targetArgs, "role show [<роль> <грейд>]"));
            default -> throw new IllegalArgumentException("Использование: role select или role show");
        }
    }

    private void selectRole(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Использование: role select <роль> <грейд>");
        }

        long userId = users.requireUser().id();
        UserTarget target = new UserTarget(args[0], Grade.parse(args[1]));
        skillProfiles.selectRole(userId, target.roleId(), target.grade());

        System.out.println("Цель сохранена.");
        showRole(target);
    }

    private void showRole(UserTarget target) {
        RoleProfile profile = roleProfiles.find(target.roleId(), target.grade()).orElse(null);
        if (profile == null) {
            System.out.println(noProfileMessage(target));
            return;
        }

        System.out.printf("%s (%s), по %d вакансиям, данные от %s%n",
                profile.roleId(), profile.grade(), profile.sampleSize(), profile.computedAt().toLocalDate());
        for (RoleProfileSkill skill : profile.skills()) {
            System.out.printf("%s — в %.0f%% вакансий, нужен уровень %d/6%n",
                    dictionary.nameOf(skill.skillId()), skill.weight() * 100, skill.requiredLevel());
        }
    }

    private void fill(String[] args) {
        long userId = users.requireUser().id();
        UserTarget target = targetOrCurrent(args, "fill [<роль> <грейд>]");
        RoleProfile profile = roleProfiles.find(target.roleId(), target.grade())
                .orElseThrow(() -> new IllegalArgumentException(noProfileMessage(target)));
        List<RoleProfileSkill> unfilled = skillProfiles.unfilledSkills(userId, profile);

        if (unfilled.isEmpty()) {
            System.out.println("Все навыки этой роли уже оценены.");
            return;
        }

        System.out.print(LEVEL_SCALE);
        for (RoleProfileSkill skill : unfilled) {
            while (true) {
                System.out.println(LEVEL_HINT);
                System.out.print(dictionary.nameOf(skill.skillId()) + " (Enter — пропуск, q — выход, ? — шкала): ");
                if (!scanner.hasNextLine()) {
                    return;
                }

                String answer = scanner.nextLine().strip();
                if (answer.equalsIgnoreCase("q")) {
                    return;
                }
                if (answer.isEmpty()) {
                    break;
                }
                if (answer.equals("?")) {
                    System.out.print(LEVEL_SCALE);
                    continue;
                }

                int level;
                try {
                    level = parseLevel(answer);
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                    continue;
                }

                skillProfiles.setLevel(userId, skill.skillId(), level, SkillSource.MANUAL);
                break;
            }
        }
    }

    private void skill(String[] args) {
        if (args.length < 3 || !args[0].equalsIgnoreCase("set")) {
            throw new IllegalArgumentException("Использование: skill set <навык> <уровень>");
        }

        long userId = users.requireUser().id();
        int level = parseLevel(args[args.length - 1]);
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));
        long skillId = skillProfiles.setLevelByName(userId, name, level);
        System.out.printf("%s — сохранён уровень %d/6%n", dictionary.nameOf(skillId), level);
    }

    private void profile(String[] args) {
        if (args.length != 0) {
            throw new IllegalArgumentException("Использование: profile");
        }

        long userId = users.requireUser().id();
        skillProfiles.getTarget(userId).ifPresentOrElse(
                target -> System.out.printf("Цель: %s (%s)%n", target.roleId(), target.grade()),
                () -> System.out.println("Выберите цель: role select <роль> <грейд>")
        );
        System.out.println("Часов в неделю: " + skillProfiles.getAvailableTimeWeekly(userId));

        List<UserSkill> skills = skillProfiles.getSkills(userId);
        if (skills.isEmpty()) {
            System.out.println("Профиль навыков пока пуст.");
            return;
        }

        for (UserSkill skill : skills) {
            System.out.printf("%s — %d/6 (%s)", dictionary.nameOf(skill.skillId()),
                    skill.level(), skill.source());
            if (skill.tested()) {
                System.out.printf(", по тесту %d/6", skill.testLevel());
            }
            System.out.println();
        }
    }

    private void match(String[] args) {
        long userId = users.requireUser().id();
        UserTarget target = targetOrCurrent(args, "match [<роль> <грейд>]");
        RoleProfile profile = roleProfiles.find(target.roleId(), target.grade())
                .orElseThrow(() -> new IllegalArgumentException(noProfileMessage(target)));

        MatchResult result = matching.calculate(profile, skillProfiles.getSkills(userId));
        matchView.print(profile, result);
    }

    private UserTarget targetOrCurrent(String[] args, String usage) {
        if (args.length == 2) {
            return new UserTarget(args[0], Grade.parse(args[1]));
        }
        if (args.length != 0) {
            throw new IllegalArgumentException("Использование: " + usage);
        }

        return skillProfiles.getTarget(users.requireUser().id())
                .orElseThrow(() -> new IllegalArgumentException("Выберите цель: role select <роль> <грейд>"));
    }

    private int parseLevel(String text) {
        int level;
        try {
            level = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Введите целый уровень от 0 до 6.");
        }
        if (level < 0 || level > 6) {
            throw new IllegalArgumentException("Уровень должен быть от 0 до 6.");
        }
        return level;
    }

    private String noProfileMessage(UserTarget target) {
        return "Нет данных по роли " + target.roleId() + " (" + target.grade() + ").";
    }
}