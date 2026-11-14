
package navigator.vacancy;

import navigator.common.Grade;
import navigator.analysis.VacancyAnalysisService;

import java.util.Scanner;

public class VacancyController {

    private final VacancyService vacancyService;
    private final VacancyRepository repository;
    private final VacancyAnalysisService vacancyAnalysisService;
    private final Scanner scanner;

    public VacancyController(
            VacancyService vacancyService,
            VacancyRepository repository,
            VacancyAnalysisService vacancyAnalysisService,
            Scanner scanner
    ) {
        this.vacancyService = vacancyService;
        this.repository = repository;
        this.vacancyAnalysisService = vacancyAnalysisService;
        this.scanner = scanner;
    }

    public void start() {

        System.out.println("Введите профессию:");
        String profession = scanner.nextLine();

        System.out.println("""
                Выберите уровень:
                1 — Junior
                2 — Middle
                3 — Senior
                """);

        int gradeNumber;

        try {
            gradeNumber = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Уровень необходимо выбрать числом: 1, 2 или 3"
            );
        }

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

        System.out.println("Начинаем сбор вакансий из SuperJob...");

        try {
            // ЭТАП 1. Сбор и сохранение вакансий
            int collected = vacancyService.collectVacancies(params);

            System.out.println(
                    "Сбор завершён. Получено вакансий: " + collected
            );

            // ЭТАП 2. Автоматический AI-анализ
            if (collected > 0) {
                System.out.println(
                        "Начинаем анализ вакансий с помощью GigaChat..."
                );

                vacancyAnalysisService.analyzeVacancies();

                System.out.println(
                        "AI-анализ вакансий завершён."
                );
            } else {
                System.out.println(
                        "Новых вакансий нет. AI-анализ не требуется."
                );
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            System.err.println(
                    "Анализ вакансий был прерван."
            );

        } catch (Exception e) {
            System.err.println(
                    "Ошибка при сборе или анализе вакансий: "
                            + e.getMessage()
            );

            e.printStackTrace();
        }
    }
}
