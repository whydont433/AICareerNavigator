package navigator.user;

import java.util.ArrayList;
import java.util.List;

public class MemoryUserRepository implements UserRepository {
    private final List<User> users = new ArrayList<>();
    private long nextId = 1;

    @Override
    public boolean exists(String name) {
        for (User u : users) {
            if (u.name().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public User get(String name) {
        for (User u : users) {
            if (u.name().equalsIgnoreCase(name)) {
                return u;
            }
        }
        throw new IllegalStateException("Пользователь не найден: " + name);
    }

    @Override
    public User create(String name) {
        User user = new User(nextId++, name);
        users.add(user);
        return user;
    }
}