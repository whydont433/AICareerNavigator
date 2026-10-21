package navigator.user;

public class UserService {
    private final UserRepository repository;
    private User currentUser;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User register(String rawName) {
        String name = validate(rawName);
        if (repository.exists(name)) {
            throw new IllegalArgumentException("Имя \"" + name + "\" уже занято. Войти: login " + name);
        }
        currentUser = repository.create(name);
        return currentUser;
    }

    public User login(String rawName) {
        String name = validate(rawName);
        if (!repository.exists(name)) {
            throw new IllegalArgumentException(
                    "Пользователь \"" + name + "\" не найден. Регистрация: register " + name);
        }
        currentUser = repository.get(name);
        return currentUser;
    }

    public void logout() {
        if (currentUser == null) {
            throw new IllegalArgumentException("Вы не вошли");
        }
        currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User requireUser() {
        if (currentUser == null) {
            throw new IllegalArgumentException("Сначала войдите: login <имя>");
        }
        return currentUser;
    }

    private String validate(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (!name.matches("[\\p{L}\\p{N}_-]{2,32}")) {
            throw new IllegalArgumentException("Имя: 2–32 символа, буквы, цифры, _ или -, без пробелов");
        }
        return name;
    }
}