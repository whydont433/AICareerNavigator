package navigator.vacancy;

import navigator.common.Grade;

import java.util.Scanner;

public class VacancyController {

    private final VacancyService vacancyService;
    private final VacancyRepository repository;

    public VacancyController(
            VacancyService vacancyService,
            VacancyRepository repository
    ) {
        this.vacancyService = vacancyService;
        this.repository = repository;
    }

    public void start() {

        Scanner scanner = new Scanner(System.in);

        System.out.println("Введите профессию:");
        String profession = scanner.nextLine();

        System.out.println("""
                Выберите уровень:
                1 — Junior
                2 — Middle
                3 — Senior
                """);

        int gradeNumber = scanner.nextInt();

        Grade grade = switch (gradeNumber) {
            case 1 -> Grade.JUNIOR;
            case 2 -> Grade.MIDDLE;
            case 3 -> Grade.SENIOR;
            default -> throw new IllegalArgumentException(
                    "Некорректный уровень"
            );
        };

        SearchParams params = new SearchParams(
                profession,
                grade
        );

        try {

            System.out.println(
                    "Начинаем сбор вакансий из SuperJob..."
            );

            int collected =
                    vacancyService.collectVacancies(params);

            System.out.println(
                    "Сбор завершён. Получено вакансий: "
                            + collected
            );



        } catch (Exception e) {

            System.err.println(
                    "Ошибка при сборе вакансий: " + e.getMessage()
            );
        }
    }
}