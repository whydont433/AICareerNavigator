package navigator;

import navigator.user.MemoryUserRepository;
import navigator.user.UserController;
import navigator.user.UserService;

import java.util.Arrays;
import java.util.TreeMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        UserService userService = new UserService(new MemoryUserRepository());
        UserController userController = new UserController(userService);

        Map<String, Command> commands = new TreeMap<>();
        addCommands(commands, userController.commands());

        Scanner in = new Scanner(System.in);
        System.out.println("AI Career Navigator. Команды: " + commands.keySet() + ", exit");

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
            Command command = commands.get(parts[0].toLowerCase());
            if (command == null) {
                System.out.println("Неизвестная команда");
                continue;
            }

            try {
                command.execute(Arrays.copyOfRange(parts, 1, parts.length));
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static void addCommands(Map<String, Command> all, Map<String, Command> fromModule) {
        for (String name : fromModule.keySet()) {
            if (all.containsKey(name)) {
                throw new IllegalStateException("Команда «" + name + "» уже занята другим модулем");
            }
            all.put(name, fromModule.get(name));
        }
    }
}