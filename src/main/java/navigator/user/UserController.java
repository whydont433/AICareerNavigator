package navigator.user;

import navigator.Command;
import navigator.vacancy.VacancyController;

import java.util.Map;

public class UserController {
    private final UserService service;
    private final VacancyController vacancyController;

    public UserController(
            UserService service,
            VacancyController vacancyController
    ) {
        this.service = service;
        this.vacancyController = vacancyController;
    }
    public Map<String, Command> commands() {
        return Map.of(
                "register", this::register,
                "login", this::login,
                "logout", this::logout,
                "whoami", this::whoami,
                "find-vacancy", this::findVacancy
        );
    }

    private void register(String[] args) {
        User u = service.register(oneArg(args, "register <имя>"));
        System.out.println("Готово, " + u.name() + "! Вы вошли.");
    }

    private void login(String[] args) {
        User u = service.login(oneArg(args, "login <имя>"));
        System.out.println("Привет, " + u.name() + "!");
    }

    private void logout(String[] args) {
        service.logout();
        System.out.println("Вы вышли.");
    }

    private void whoami(String[] args) {
        System.out.println(service.isLoggedIn() ? "Вы: " + service.requireUser().name() : "Вы не вошли.");
    }

    private void findVacancy(String[] args) {
        if (args.length != 0) {
            throw new IllegalArgumentException(
            );
        }

        if (!service.isLoggedIn()) {
            System.out.println("Сначала необходимо войти в аккаунт.");
            return;
        }

        vacancyController.start();
    }

    private String oneArg(String[] args, String usage) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Использование: " + usage);
        }
        return args[0];
    }
}