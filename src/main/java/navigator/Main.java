
package navigator;

import navigator.analysis.PostgresVacancyAnalysisRepository;
import navigator.analysis.VacancyAnalysisRepository;
import navigator.config.DatabaseConfig;

import navigator.user.DbUserRepository;
import navigator.user.UserController;
import navigator.user.UserService;

import navigator.vacancy.*;

import navigator.analysis.VacancyAnalysisService;
import navigator.skills.SkillExtractionService;

import navigator.gigachat.GigaChatClient;

import navigator.skills.SkillRepository;
import navigator.skills.PostgresSkillRepository;
import navigator.skills.SkillSynonymDictionary;
import navigator.skills.SkillNormalizationService;

import navigator.trajectory.TrajectoryController;
import navigator.trajectory.external.RoleCatalog;
import navigator.trajectory.external.SkillDictionary;
import navigator.trajectory.external.PostgresRoleCatalog;
import navigator.trajectory.external.PostgresSkillDictionary;
import navigator.trajectory.external.PostgresVacancyReader;
import navigator.trajectory.external.VacancyReader;
import navigator.trajectory.matching.MatchingService;
import navigator.trajectory.profile.PostgresUserSkillRepository;
import navigator.trajectory.profile.PostgresUserTargetRepository;
import navigator.trajectory.profile.SkillProfileService;
import navigator.trajectory.profile.UserSkillRepository;
import navigator.trajectory.role.PostgresRoleProfileRepository;
import navigator.trajectory.role.RoleProfileRepository;
import navigator.trajectory.role.RoleProfileService;

import java.util.Arrays;
import java.util.TreeMap;
import java.util.Map;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        // Один Scanner для всего консольного приложения
        Scanner in = new Scanner(System.in);

        // =========================================
        // 1. Подключение к базе данных
        // =========================================

        DatabaseConfig databaseConfig = new DatabaseConfig();

        // =========================================
        // 2. Создание пользовательского модуля
        // =========================================

        UserService userService = new UserService(
                new DbUserRepository(
                        "jdbc:postgresql://localhost:5432/career_navigator",
                        "postgres",
                        "postgres"
                )
        );

        // =========================================
        // 3. Создание модуля сбора вакансий
        // =========================================

        VacancyRepository vacancyRepository =
                new PostgresVacancyRepository(databaseConfig);

        VacancyService vacancyService =
                new VacancyService(vacancyRepository);

        // =========================================
        // 4. Создание модуля AI-анализа вакансий
        // =========================================

        // Клиент GigaChat
        GigaChatClient gigaChatClient = new GigaChatClient();

        // Сервис извлечения навыков из текста вакансии
        SkillExtractionService extractionService =
                new SkillExtractionService(gigaChatClient);

        // Сервис нормализации названий навыков
        SkillSynonymDictionary synonymDictionary =
                new SkillSynonymDictionary();

        SkillNormalizationService normalizationService =
                new SkillNormalizationService(synonymDictionary);

        // Репозиторий навыков
        SkillRepository skillRepository =
                new PostgresSkillRepository();

        // Репозиторий для получения вакансий и сохранения извлечённых навыков
        VacancyAnalysisRepository analysisRepository =
                new PostgresVacancyAnalysisRepository(skillRepository);

        // Основной сервис AI-анализа
        VacancyAnalysisService vacancyAnalysisService =
                new VacancyAnalysisService(
                        analysisRepository,
                        extractionService,
                        normalizationService
                );

        // =========================================
        // 5. Создание контроллеров
        // =========================================

        VacancyController vacancyController =
                new VacancyController(
                        vacancyService,
                        vacancyRepository,
                        vacancyAnalysisService,
                        in
                );

        UserController userController =
                new UserController(
                        userService,
                        vacancyController
                );

        // Модуль траектории навыков
        RoleCatalog roleCatalog = new PostgresRoleCatalog();
        SkillDictionary skillDictionary = new PostgresSkillDictionary(synonymDictionary);
        RoleProfileRepository roleProfileRepository = new PostgresRoleProfileRepository();
        UserSkillRepository userSkillRepository = new PostgresUserSkillRepository();
        VacancyReader trajectoryVacancies = new PostgresVacancyReader();

        RoleProfileService roleProfileService =
                new RoleProfileService(roleProfileRepository, trajectoryVacancies, roleCatalog);

        SkillProfileService skillProfileService = new SkillProfileService(
                userSkillRepository, new PostgresUserTargetRepository(), roleCatalog, skillDictionary
        );

        MatchingService matchingService =
                new MatchingService(roleProfileRepository, userSkillRepository);

        TrajectoryController trajectoryController = new TrajectoryController(
                userService, roleCatalog, roleProfileService,
                skillProfileService, matchingService, skillDictionary, in
        );

        // =========================================
        // 6. Регистрация команд
        // =========================================

        Map<String, Command> commands = new TreeMap<>();

        addCommands(commands, userController.commands());
        addCommands(commands, trajectoryController.commands());


        // =========================================
        // 7. Запуск консольного приложения
        // =========================================

        System.out.println(
                "AI Career Navigator. Команды: "
                        + commands.keySet()
                        + ", exit"
        );

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

            Command command =
                    commands.get(parts[0].toLowerCase());

            if (command == null) {
                System.out.println("Неизвестная команда");
                continue;
            }

            try {
                command.execute(
                        Arrays.copyOfRange(parts, 1, parts.length)
                );

            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());

            } catch (Exception e) {
                System.err.println(
                        "Ошибка выполнения команды: " + e.getMessage()
                );
                e.printStackTrace();
            }
        }

        System.out.println("Приложение завершено.");
    }

    private static void addCommands(
            Map<String, Command> all,
            Map<String, Command> fromModule
    ) {

        for (String name : fromModule.keySet()) {

            if (all.containsKey(name)) {
                throw new IllegalStateException(
                        "Команда «" + name
                                + "» уже занята другим модулем"
                );
            }

            all.put(name, fromModule.get(name));
        }
    }
}