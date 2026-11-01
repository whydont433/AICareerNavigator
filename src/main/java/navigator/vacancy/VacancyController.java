package navigator.vacancy;




import navigator.common.Grade;

import java.util.List;
import java.util.Scanner;

public class VacancyController {

    private final navigator.vacancy.VacancyService vacancyService;
    private final VacancyRepository repository;

    public VacancyController(
            navigator.vacancy.VacancyService vacancyService,
            VacancyRepository repository
    ) {
        this.vacancyService = vacancyService;
        this.repository = repository;
    }

    public void start() throws Exception {

        Scanner scanner = new Scanner(System.in);

        System.out.print("Введите интересующую вас профессию: ");
        String profession = scanner.nextLine();

        System.out.println("Выберите уровень:");
        System.out.println("1 - Junior");
        System.out.println("2 - Middle");
        System.out.println("3 - Senior");

        int choice = Integer.parseInt(scanner.nextLine());

        Grade grade;

        switch (choice) {
            case 1:
                grade = Grade.JUNIOR;
                break;

            case 2:
                grade = Grade.MIDDLE;
                break;

            case 3:
                grade = Grade.SENIOR;
                break;

            default:
                System.out.println("Неверный уровень");
                return;
        }

        SearchParams params = new SearchParams(
                profession,
                grade,
                30,
                250
        );


        int collected = vacancyService.collectVacancies(params);

    }
}