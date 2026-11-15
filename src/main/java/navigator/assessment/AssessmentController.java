package navigator.assessment;

import navigator.Command;
import navigator.db.DbSkillDictionary;
import navigator.trajectory.ai.SkillImportService;
import navigator.trajectory.api.RoleProfile;
import navigator.trajectory.api.RoleProfileApi;
import navigator.trajectory.api.RoleProfileSkill;
import navigator.trajectory.api.SkillSource;
import navigator.trajectory.api.UserSkill;
import navigator.trajectory.api.UserTarget;
import navigator.trajectory.external.RoleCatalog;
import navigator.trajectory.external.RoleInfo;
import navigator.trajectory.profile.SkillProfileService;
import navigator.user.User;
import navigator.user.UserService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

public class AssessmentController {
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";
    private static final int BOSS_SKILLS = 3;

    private final UserService userService;
    private final SkillProfileService skillProfileService;
    private final RoleProfileApi roleProfileApi;
    private final RoleCatalog roleCatalog;
    private final DbSkillDictionary skillDictionary;
    private final TestService testService;
    private final TestRepository testRepository;
    private final SkillImportService skillImportService;
    private final Scanner scanner;

    public AssessmentController(UserService userService,
                                SkillProfileService skillProfileService,
                                RoleProfileApi roleProfileApi,
                                RoleCatalog roleCatalog,
                                DbSkillDictionary skillDictionary,
                                TestService testService,
                                TestRepository testRepository,
                                SkillImportService skillImportService,
                                Scanner scanner) {
        this.userService = userService;
        this.skillProfileService = skillProfileService;
        this.roleProfileApi = roleProfileApi;
        this.roleCatalog = roleCatalog;
        this.skillDictionary = skillDictionary;
        this.testService = testService;
        this.testRepository = testRepository;
        this.skillImportService = skillImportService;
        this.scanner = scanner;
    }

    public Map<String, Command> commands() {
        Map<String, Command> map = new LinkedHashMap<>();
        map.put("test", this::handleTest);
        map.put("boss", this::handleBoss);
        map.put("skill-import", this::handleSkillImport);
        return map;
    }

    public void handleSkillImport(String[] args) {
        User user = userService.requireUser();
        System.out.println("Введите текст о себе и своём опыте (до 3000 символов):");
        if (!scanner.hasNextLine()) {
            return;
        }
        String text = scanner.nextLine();
        List<SkillImportService.ImportedSkill> extracted;
        try {
            extracted = skillImportService.extractSkills(text, skillDictionary.findAllMap());
        } catch (Exception e) {
            System.out.println("Не удалось разобрать текст, введите данные вручную");
            return;
        }
        if (extracted.isEmpty()) {
            System.out.println("Подтверждённых навыков в тексте не найдено.");
            return;
        }
        for (SkillImportService.ImportedSkill item : extracted) {
            System.out.println("Найдено: " + item.skillName() + " -> уровень " + item.level()
                    + " (цитата: «" + item.evidence() + "»)");
            System.out.print("Принять (y), отклонить (n) или ввести свой уровень (0-6): ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String answer = scanner.nextLine().trim().toLowerCase();
            if (answer.equals("y") || answer.equals("д")) {
                skillProfileService.setLevel(user.id(), item.skillId(), item.level(), SkillSource.AI);
                System.out.println("Сохранено: " + item.skillName() + " = " + item.level());
            } else if (answer.matches("[0-6]")) {
                int custom = Integer.parseInt(answer);
                skillProfileService.setLevel(user.id(), item.skillId(), custom, SkillSource.AI);
                System.out.println("Сохранено: " + item.skillName() + " = " + custom);
            }
        }
    }

    private void handleTest(String[] args) {
        User user = userService.requireUser();
        Optional<UserTarget> targetOpt = skillProfileService.getTarget(user.id());
        if (targetOpt.isEmpty()) {
            System.out.println("Сначала выберите роль: role select <роль> <грейд>");
            return;
        }
        UserTarget target = targetOpt.get();
        Optional<RoleProfile> roleProfileOpt = roleProfileApi.find(target.roleId(), target.grade());

        if (args.length == 0) {
            if (roleProfileOpt.isEmpty()) {
                System.out.println("Нет данных по этой роли и грейду");
                return;
            }
            System.out.println("Навыки роли " + resolveRoleName(target.roleId()) + " (" + target.grade() + "):");
            for (RoleProfileSkill rs : roleProfileOpt.get().skills()) {
                Optional<Integer> last = testRepository.findLastTestLevel(user.id(), rs.skillId());
                String lastText = last.map(lvl -> "последний тест: " + lvl + "/6").orElse("тест не пройден");
                System.out.println("  " + skillDictionary.nameOf(rs.skillId())
                        + " (требуется " + rs.requiredLevel() + ") — " + lastText);
            }
            return;
        }

        String skillQuery = String.join(" ", args);
        List<Long> found = skillDictionary.find(skillQuery);
        if (found.isEmpty()) {
            System.out.println("Навык «" + skillQuery + "» не найден.");
            return;
        }
        if (found.size() > 1) {
            String names = found.stream().map(skillDictionary::nameOf).collect(Collectors.joining(", "));
            System.out.println("«" + skillQuery + "» — это несколько навыков: " + names);
            return;
        }

        long skillId = found.get(0);
        Integer requiredLevel = roleProfileOpt.flatMap(rp -> rp.skills().stream()
                .filter(s -> s.skillId() == skillId)
                .map(RoleProfileSkill::requiredLevel)
                .findFirst()).orElse(null);

        runSingleSkillTest(user.id(), skillId, target, requiredLevel);
    }

    private void handleBoss(String[] args) {
        User user = userService.requireUser();
        Optional<UserTarget> targetOpt = skillProfileService.getTarget(user.id());
        if (targetOpt.isEmpty()) {
            System.out.println("Сначала выберите роль: role select <роль> <грейд>");
            return;
        }
        UserTarget target = targetOpt.get();
        Optional<RoleProfile> roleProfileOpt = roleProfileApi.find(target.roleId(), target.grade());
        if (roleProfileOpt.isEmpty()) {
            System.out.println("Нет данных по этой роли и грейду");
            return;
        }

        List<RoleProfileSkill> topSkills = roleProfileOpt.get().skills().stream()
                .sorted(Comparator.comparingDouble(RoleProfileSkill::weight).reversed()
                        .thenComparingLong(RoleProfileSkill::skillId))
                .limit(BOSS_SKILLS)
                .toList();

        if (topSkills.isEmpty()) {
            System.out.println("Нет навыков для испытания.");
            return;
        }

        System.out.println("=== Испытание «Карьерный босс»: " + resolveRoleName(target.roleId())
                + " (" + target.grade() + ") ===");

        boolean allPassed = true;
        for (RoleProfileSkill rs : topSkills) {
            Optional<TestResult> res = runSingleSkillTest(user.id(), rs.skillId(), target, rs.requiredLevel());
            if (res.isEmpty()) {
                System.out.println("Испытание прервано.");
                return;
            }
            if (res.get().testLevel() < rs.requiredLevel()) {
                allPassed = false;
            }
        }

        if (allPassed) {
            System.out.println(GREEN + "Босс побеждён! Все ключевые навыки подтверждены." + RESET);
        } else {
            System.out.println(RED + "Босс не пройден. Подтяните пробелы и попробуйте снова." + RESET);
        }
    }

    private Optional<TestResult> runSingleSkillTest(long userId, long skillId, UserTarget target, Integer roleRequiredLevel) {
        String skillName = skillDictionary.nameOf(skillId);
        Integer currentUserLevel = skillProfileService.getSkills(userId).stream()
                .filter(s -> s.skillId() == skillId)
                .map(UserSkill::level)
                .findFirst()
                .orElse(null);

        int targetLevel = testService.resolveTargetLevel(roleRequiredLevel, currentUserLevel);
        int[] window = testService.calculateWindow(targetLevel);

        List<TestQuestion> questions;
        try {
            questions = testService.prepareQuestions(userId, skillId, skillName,
                    resolveRoleName(target.roleId()), target.grade(), targetLevel);
        } catch (Exception e) {
            System.out.println("Не удалось подготовить тест по навыку, попробуйте позже");
            return Optional.empty();
        }

        long attemptId = testService.startAttempt(userId, skillId, targetLevel, window[0], window[1]);
        List<Integer> answers = new ArrayList<>();

        System.out.println("\nТест по навыку: " + skillName + " (уровни " + window[0] + "–" + window[1] + ")");
        System.out.println("Ответ: 1–4, s — пропустить, q — выйти");

        for (int i = 0; i < questions.size(); i++) {
            TestQuestion q = questions.get(i);
            System.out.println("\nВопрос " + (i + 1) + "/" + questions.size() + " [Уровень " + q.level() + "]: " + q.question());
            for (int optIdx = 0; optIdx < q.options().size(); optIdx++) {
                System.out.println("  " + (optIdx + 1) + ") " + q.options().get(optIdx));
            }

            while (true) {
                System.out.print("Ваш ответ: ");
                if (!scanner.hasNextLine()) {
                    testService.abandonTest(attemptId);
                    return Optional.empty();
                }
                String input = scanner.nextLine().trim().toLowerCase();
                if (input.equals("q")) {
                    testService.abandonTest(attemptId);
                    System.out.println("Тест прерван.");
                    return Optional.empty();
                }
                if (input.equals("s")) {
                    answers.add(null);
                    break;
                }
                if (input.matches("[1-4]")) {
                    answers.add(Integer.parseInt(input) - 1);
                    break;
                }
                System.out.println("Введите 1, 2, 3, 4, s или q.");
            }
        }

        int reqForDisplay = roleRequiredLevel != null ? roleRequiredLevel : targetLevel;
        TestResult result = testService.completeTest(
                attemptId,
                userId,
                skillId,
                reqForDisplay,
                window[0],
                questions,
                answers,
                res -> printTestResult(skillName, res)
        );

        return Optional.of(result);
    }

    private void printTestResult(String skillName, TestResult result) {
        System.out.println("\nРезультат теста: " + skillName + " — верно " + result.correct() + " из " + result.total());
        for (Map.Entry<Integer, Integer> entry : result.correctByLevel().entrySet()) {
            boolean passed = entry.getValue() >= TestService.PASS_THRESHOLD;
            String mark = passed ? GREEN + "✓" + RESET : RED + "✗" + RESET;
            System.out.println("  Уровень " + entry.getKey() + "  " + mark + "  " + entry.getValue() + "/" + TestService.QUESTIONS_PER_LEVEL);
        }

        String bar = "[" + "█".repeat(result.testLevel()) + "░".repeat(6 - result.testLevel()) + "]";
        boolean meetsRequired = result.testLevel() >= result.requiredLevel();
        String reqMark = meetsRequired ? GREEN + "✓" + RESET : RED + "✗" + RESET;
        System.out.println("Уровень по тесту: " + result.testLevel() + " из 6  " + bar + "  "
                + result.testLevel() + "/" + result.requiredLevel() + " " + reqMark);

        if (!result.wrongAnswers().isEmpty()) {
            System.out.println("\nРазбор ошибок:");
            for (TestResult.WrongAnswer w : result.wrongAnswers()) {
                System.out.println("  • Вопрос: " + w.question());
                System.out.println("    Ваш ответ: " + RED + w.chosenAnswer() + RESET + " | Верно: " + GREEN + w.correctAnswer() + RESET);
                System.out.println("    Пояснение: " + w.explanation());
            }
        }

        if (!result.topicsToRepeat().isEmpty()) {
            String topics = result.topicsToRepeat().entrySet().stream()
                    .map(e -> e.getKey() + " (" + e.getValue() + ")")
                    .collect(Collectors.joining(", "));
            System.out.println("Темы для повторения: " + topics);
        }
    }

    private String resolveRoleName(String roleId) {
        for (RoleInfo info : roleCatalog.all()) {
            if (info.id().equals(roleId)) {
                return info.name();
            }
        }
        return roleId;
    }
}
