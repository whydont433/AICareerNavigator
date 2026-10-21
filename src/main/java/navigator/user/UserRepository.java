package navigator.user;

public interface UserRepository {
    boolean exists(String name);

    /** вызов только при exits true **/
    User get(String name);

    User create(String name);
}