
package navigator;

import navigator.analysis.PostgresVacancyAnalysisRepository;
import navigator.analysis.VacancyAnalysisRepository;
import navigator.config.DatabaseConfig;

import navigator.flow.CareerFlowController;
import navigator.trajectory.alternative.AlternativeCareerService;
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
import navigator.skills.SkillSynonymService;

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
import navigator.trajectory.profile.UserTargetRepository;
import navigator.trajectory.profile.SkillProfileService;
import navigator.trajectory.profile.UserSkillRepository;
import navigator.trajectory.role.PostgresRoleProfileRepository;
import navigator.trajectory.role.RoleProfileRepository;
import navigator.trajectory.role.RoleProfileService;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.Map;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner in = new Scanner(System.in);

        DatabaseConfig databaseConfig = new DatabaseConfig();

        // Проверяем БД и схему до начала работы: без БД приложение не работает
        try {
            DatabaseConfig.ensureSchema();
        } catch (SQLException e) {
            System.err.println("База данных недоступна: " + e.getMessage());
            System.err.println("Проверьте, что PostgreSQL запущен (docker compose up -d), "
                    + "а DB_URL / DB_USER / DB_PASSWORD заданы верно.");
            return;
        }

        UserService userService = new UserService(
                new DbUserRepository(
                        DatabaseConfig.url(),
                        DatabaseConfig.user(),
                        DatabaseConfig.password()
                )
        );



        VacancyRepository vacancyRepository = new PostgresVacancyRepository(databaseConfig);

        VacancyService vacancyService = new VacancyService(vacancyRepository);



        // Клиент GigaChat
        GigaChatClient gigaChatClient = new GigaChatClient();

        // Сервис извлечения навыков из текста вакансии
        SkillExtractionService extractionService = new SkillExtractionService(gigaChatClient);

        // Сервис нормализации названий навыков
        SkillSynonymDictionary synonymDictionary = new SkillSynonymDictionary();

        SkillNormalizationService normalizationService = new SkillNormalizationService(synonymDictionary);

        // Агрегация синонимов навыков через GigaChat (JS = JavaScript и т.п.)
        SkillSynonymService synonymService =
                new SkillSynonymService(gigaChatClient, synonymDictionary);

        try {
            synonymService.loadKnownSynonyms();
        } catch (SQLException e) {
            System.err.println("Не удалось загрузить словарь синонимов: " + e.getMessage());
        }

        // Репозиторий навыков
        SkillRepository skillRepository = new PostgresSkillRepository();

        // Репозиторий для получения вакансий и сохранения извлечённых навыков
        VacancyAnalysisRepository analysisRepository = new PostgresVacancyAnalysisRepository(skillRepository);

        // Основной сервис AI-анализа
        VacancyAnalysisService vacancyAnalysisService =
                new VacancyAnalysisService(
                        analysisRepository,
                        extractionService,
                        normalizationService
                );



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
        UserTargetRepository userTargetRepository = new PostgresUserTargetRepository();
        VacancyReader trajectoryVacancies = new PostgresVacancyReader();

        // Модуль подбора альтернативных профессий.
        AlternativeCareerService alternativeCareerService = new AlternativeCareerService(
                userSkillRepository, roleProfileRepository, userTargetRepository, roleCatalog
        );

        RoleProfileService roleProfileService =
                new RoleProfileService(roleProfileRepository, trajectoryVacancies, roleCatalog);

        SkillProfileService skillProfileService = new SkillProfileService(
                userSkillRepository, userTargetRepository, roleCatalog, skillDictionary
        );

        MatchingService matchingService =
                new MatchingService(roleProfileRepository, userSkillRepository);

        TrajectoryController trajectoryController = new TrajectoryController(
                userService, roleCatalog, roleProfileService,
                skillProfileService, matchingService, skillDictionary, in
        );


        CareerFlowController flowController = new CareerFlowController(
                userService, vacancyService, vacancyAnalysisService, synonymService,
                roleCatalog, roleProfileService, skillProfileService,
                trajectoryController, alternativeCareerService, in
        );


        Map<String, Command> commands = new TreeMap<>();

        addCommands(commands, userController.commands());
        addCommands(commands, flowController.commands());

        System.out.println("""
                AI Career Navigator.
                Команды:
                  register <имя>      — регистрация
                  login <имя>         — вход
                  select vacancy      — выбрать профессию и оценить свои навыки
                  change my vacancy   — сменить выбранную профессию
                  report              — отчёт: профессия, навыки, соответствие, альтернативы
                  whoami, logout, exit
                """);

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

            Command command = null;
            int commandWords = 0;

            for (int n = Math.min(3, parts.length); n >= 1; n--) {
                String key = String.join(" ", Arrays.copyOfRange(parts, 0, n))
                        .toLowerCase();

                command = commands.get(key);

                if (command != null) {
                    commandWords = n;
                    break;
                }
            }

            if (command == null) {
                System.out.println("Неизвестная команда. Доступные: " + commands.keySet() + ", exit");
                continue;
            }

            try {
                command.execute(Arrays.copyOfRange(parts, commandWords, parts.length));

            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());

            } catch (Exception e) {
                System.err.println(describe(e));

                if (System.getenv("DEBUG") != null) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Приложение завершено.");
    }

    //Сообщения об ошибках
    private static String describe(Throwable error) {

        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql) {
                String state = sql.getSQLState();

                if (state == null || state.startsWith("08")) {
                    return "База данных недоступна: " + sql.getMessage();
                }

                return "Ошибка базы данных: " + sql.getMessage();
            }
        }

        String message = error.getMessage();

        return "Ошибка: " + (message == null ? error.getClass().getSimpleName() : message);
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