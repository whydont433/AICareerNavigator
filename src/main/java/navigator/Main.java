package navigator;

import navigator.ai.HttpLlmClient;
import navigator.ai.LlmClient;
import navigator.assessment.AssessmentController;
import navigator.assessment.DbTestRepository;
import navigator.assessment.TestRepository;
import navigator.assessment.TestService;
import navigator.db.DbConnectionFactory;
import navigator.db.DbRoleCatalog;
import navigator.db.DbRoleProfileRepository;
import navigator.db.DbSkillDictionary;
import navigator.db.DbUserSkillRepository;
import navigator.db.DbUserTargetRepository;
import navigator.trajectory.ai.SkillImportService;
import navigator.trajectory.api.SkillSource;
import navigator.trajectory.profile.SkillProfileService;
import navigator.trajectory.role.RoleProfileService;
import navigator.user.DbUserRepository;
import navigator.user.UserController;
import navigator.user.UserService;
import navigator.vacancy.MemoryVacancyRepository;
import navigator.vacancy.VacancyController;
import navigator.vacancy.VacancyRepository;
import navigator.vacancy.VacancyService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.TreeMap;

public class Main {
    public static void main(String[] args) {
        DbConnectionFactory db = DbConnectionFactory.defaultLocal();

        UserService userService = new UserService(new DbUserRepository(
                System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/career_navigator"),
                System.getenv().getOrDefault("DB_USER", "postgres"),
                System.getenv().getOrDefault("DB_PASSWORD", "postgres")
        ));

        VacancyRepository vacancyRepository = new MemoryVacancyRepository();
        VacancyService vacancyService = new VacancyService(vacancyRepository);
        VacancyController vacancyController = new VacancyController(vacancyService, vacancyRepository);
        UserController userController = new UserController(userService, vacancyController);

        DbSkillDictionary skillDictionary = new DbSkillDictionary(db);
        DbRoleCatalog roleCatalog = new DbRoleCatalog(db);
        DbRoleProfileRepository roleProfileRepository = new DbRoleProfileRepository(db);
        DbUserSkillRepository userSkillRepository = new DbUserSkillRepository(db);
        DbUserTargetRepository userTargetRepository = new DbUserTargetRepository(db);

        SkillProfileService skillProfileService = new SkillProfileService(
                userSkillRepository, userTargetRepository, roleCatalog, skillDictionary
        );
        RoleProfileService roleProfileService = new RoleProfileService(
                roleProfileRepository, (roleId, grade) -> List.of(), roleCatalog
        );

        LlmClient llmClient = new HttpLlmClient();
        SkillImportService skillImportService = new SkillImportService(llmClient);
        TestRepository testRepository = new DbTestRepository(db);
        TestService testService = new TestService(testRepository, llmClient);

        Scanner in = new Scanner(System.in);

        testService.onTestCompleted(event -> {
            Optional<Integer> discrepancy = skillProfileService.recordTest(
                    event.userId(), event.skillId(), event.testLevel()
            );
            if (discrepancy.isPresent()) {
                System.out.print("Вы указали " + discrepancy.get() + ", по тесту " + event.testLevel() + " — изменить? (y/n): ");
                if (in.hasNextLine()) {
                    String confirm = in.nextLine().trim().toLowerCase();
                    if (confirm.equals("y") || confirm.equals("д")) {
                        skillProfileService.setLevel(event.userId(), event.skillId(), event.testLevel(), SkillSource.TEST);
                        System.out.println("Уровень обновлён до " + event.testLevel() + ".");
                    }
                }
            }
        });

        AssessmentController assessmentController = new AssessmentController(
                userService,
                skillProfileService,
                roleProfileService,
                roleCatalog,
                skillDictionary,
                testService,
                testRepository,
                skillImportService,
                in
        );

        Map<String, Command> commands = new TreeMap<>();
        addCommands(commands, userController.commands());
        addCommands(commands, assessmentController.commands());

        System.out.println("AI Career Navigator. Команды: " + commands.keySet() + ", exit");

        while (true) {
            System.out.print("> ");
            if (!in.hasNextLine()) {
                break;
            }
            String line = in.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.equals("exit")) {
                break;
            }

            String[] parts = line.split("\\s+");
            Command command = commands.get(parts[0].toLowerCase());
            if (command == null) {
                System.out.println("Неизвестная команда");
                continue;
            }

            try {
                command.execute(Arrays.copyOfRange(parts, 1, parts.length));
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void addCommands(Map<String, Command> all, Map<String, Command> fromModule) {
        for (String name : fromModule.keySet()) {
            if (all.containsKey(name)) {
                throw new IllegalStateException("Команда «" + name + "» уже занята другим модулем");
            }
            all.put(name, fromModule.get(name));
        }
    }
}
