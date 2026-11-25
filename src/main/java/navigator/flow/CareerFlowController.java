package navigator.flow;

import navigator.Command;
import navigator.analysis.VacancyAnalysisService;
import navigator.common.Grade;
import navigator.skills.SkillSynonymService;
import navigator.trajectory.TrajectoryController;
import navigator.trajectory.alternative.AlternativeCareerService;
import navigator.trajectory.api.RoleProfile;
import navigator.trajectory.api.UserTarget;
import navigator.trajectory.external.RoleCatalog;
import navigator.trajectory.external.RoleInfo;
import navigator.trajectory.profile.SkillProfileService;
import navigator.trajectory.role.RoleProfileService;
import navigator.user.User;
import navigator.user.UserService;
import navigator.vacancy.SearchParams;
import navigator.vacancy.VacancyService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;


public class CareerFlowController {

    private final UserService users;
    private final VacancyService vacancyService;
    private final VacancyAnalysisService analysisService;
    private final SkillSynonymService synonymService;
    private final RoleCatalog roles;
    private final RoleProfileService roleProfiles;
    private final SkillProfileService skillProfiles;
    private final TrajectoryController trajectory;
    private final AlternativeCareerService alternatives;
    private final Scanner scanner;

    public CareerFlowController(
            UserService users,
            VacancyService vacancyService,
            VacancyAnalysisService analysisService,
            SkillSynonymService synonymService,
            RoleCatalog roles,
            RoleProfileService roleProfiles,
            SkillProfileService skillProfiles,
            TrajectoryController trajectory,
            AlternativeCareerService alternatives,
            Scanner scanner
    ) {
        this.users = users;
        this.vacancyService = vacancyService;
        this.analysisService = analysisService;
        this.synonymService = synonymService;
        this.roles = roles;
        this.roleProfiles = roleProfiles;
        this.skillProfiles = skillProfiles;
        this.trajectory = trajectory;
        this.alternatives = alternatives;
        this.scanner = scanner;
    }

    public Map<String, Command> commands() {
        return Map.of(
                "select vacancy", this::selectVacancy,
                "change my vacancy", this::changeVacancy,
                "report", this::report
        );
    }

    private void selectVacancy(String[] args) {
        User user = users.requireUser();
        runSelection(user, args);
    }

    private void changeVacancy(String[] args) {
        User user = users.requireUser();
        UserTarget current = skillProfiles.getTarget(user.id())
                .orElseThrow(() -> new IllegalArgumentException("Вакансия ещё не выбрана. Используйте: select vacancy"));

        System.out.println("Текущая вакансия: " + roleName(current.roleId()) + " (" + current.grade() + ")");
        runSelection(user, args);
    }

    private void runSelection(User user, String[] args) {

        String profession;
        Grade grade;

        if (args.length > 0) {
            Grade lastAsGrade = tryParseGrade(args[args.length - 1]);
            int professionWords = lastAsGrade != null ? args.length - 1 : args.length;

            if (professionWords == 0) {
                throw new IllegalArgumentException("Использование: select vacancy <профессия> [junior|middle|senior]");
            }

            profession = String.join(" ", Arrays.copyOfRange(args, 0, professionWords));
            grade = lastAsGrade != null ? lastAsGrade : askGrade();
        } else {
            System.out.println("Введите профессию:");
            profession = readLine();
            grade = askGrade();
        }

        SearchParams params = new SearchParams(resolveProfessionName(profession), grade);

        System.out.println("Подождите, идёт сбор реальных вакансий...");

        boolean searchFailed = false;
        int collected = 0;

        try {
            collected = vacancyService.collectVacancies(params);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Поиск прерван.");
            return;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка базы данных при сохранении вакансий", e);
        } catch (IOException | RuntimeException e) {
            searchFailed = true;
            System.out.println("Ошибка получения вакансий: " + e.getMessage());
        }

        Optional<String> roleId = findRoleId(params.profession());

        if (roleId.isEmpty()) {
            System.out.println("Вакансий по запросу «" + params.profession() + "» не найдено. Попробуйте изменить название профессии.");
            return;
        }


        int analyzed = 0;

        try {
            VacancyAnalysisService.Summary summary =
                    analysisService.analyzeVacancies(Long.parseLong(roleId.get()), grade);

            analyzed = summary.success();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Анализ прерван.");
            return;
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка базы данных при анализе вакансий", e);
        } catch (IllegalStateException e) {
            System.out.println("Ошибка: " + e.getMessage());
        }

        if (analyzed > 0) {
            try {
                synonymService.aggregate();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Анализ прерван.");
                return;
            } catch (SQLException | IOException | RuntimeException e) {
                System.out.println("Не удалось объединить синонимы навыков: " + e.getMessage());
            }
        }

        Optional<RoleProfile> profile = roleProfiles.rebuild(roleId.get(), grade);

        if (profile.isEmpty()) {
            System.out.printf(
                    "Недостаточно данных по профессии: разобрано вакансий — %d, "
                            + "нужно минимум 3 за последние 30 дней.%n"
                            + (collected == 0 && searchFailed
                            ? "Проверьте доступ к SuperJob и повторите команду.%n"
                            : "Проверьте доступ к GigaChat и повторите команду.%n"),
                    roleProfiles.analyzedCount(roleId.get(), grade));
            return;
        }

        UserTarget target = new UserTarget(roleId.get(), grade);
        skillProfiles.selectRole(user.id(), target.roleId(), target.grade());


        System.out.println("\nВакансия выбрана: " + roleName(target.roleId()) + " (" + grade + ").");
        System.out.println("Оцените свои навыки по шкале 0–6:");

        trajectory.fillSkills(target);

        System.out.println("\nГотово! Введите report, чтобы увидеть отчёт.");
    }



    private void report(String[] args) {

        if (args.length != 0) {
            throw new IllegalArgumentException("Использование: report");
        }

        User user = users.requireUser();

        UserTarget target = skillProfiles.getTarget(user.id())
                .orElseThrow(() -> new IllegalArgumentException("Вакансия ещё не выбрана. Используйте: select vacancy"));

        System.out.println("===== Пользователь =====");
        System.out.println("Имя: " + user.name());
        trajectory.showProfile();

        System.out.println("\n===== Профессия и требуемые навыки =====");
        trajectory.showRole(target);

        System.out.println("\n===== Соответствие профессии =====");
        trajectory.showMatch(target);

        System.out.println("\n===== Альтернативные карьерные маршруты =====");
        alternatives.printAlternatives(user.id());
    }


    private Grade askGrade() {
        System.out.println("""
                Выберите уровень:
                1 — Junior
                2 — Middle
                3 — Senior""");

        Grade grade = tryParseGrade(readLine());

        if (grade == null) {
            throw new IllegalArgumentException("Уровень: 1, 2, 3 или junior / middle / senior");
        }

        return grade;
    }

    private static Grade tryParseGrade(String text) {

        if (text == null) {
            return null;
        }

        return switch (text.trim().toLowerCase(Locale.ROOT)) {
            case "1", "junior" -> Grade.JUNIOR;
            case "2", "middle" -> Grade.MIDDLE;
            case "3", "senior" -> Grade.SENIOR;
            default -> null;
        };
    }

    private String readLine() {
        if (!scanner.hasNextLine()) {
            throw new IllegalArgumentException("Ввод завершён");
        }
        return scanner.nextLine().trim();
    }

    private String resolveProfessionName(String input) {

        String trimmed = input == null ? "" : input.trim();

        for (RoleInfo role : roles.all()) {
            if (role.name().equalsIgnoreCase(trimmed)) {
                return role.name();
            }
        }

        return trimmed;
    }

    private Optional<String> findRoleId(String professionName) {

        for (RoleInfo role : roles.all()) {
            if (role.name().equals(professionName)) {
                return Optional.of(role.id());
            }
        }

        return Optional.empty();
    }

    private String roleName(String roleId) {

        for (RoleInfo role : roles.all()) {
            if (role.id().equals(roleId)) {
                return role.name();
            }
        }

        return roleId;
    }
}
