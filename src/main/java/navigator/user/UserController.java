package navigator.user;

import navigator.Command;

import java.util.Map;

public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    public Map<String, Command> commands() {
        return Map.of(
                "register", this::register,
                "login", this::login,
                "logout", this::logout,
                "whoami", this::whoami
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

    private String oneArg(String[] args, String usage) {
        if (args.length != 1) {
            throw new IllegalArgumentException("Использование: " + usage);
        }
        return args[0];
    }
}