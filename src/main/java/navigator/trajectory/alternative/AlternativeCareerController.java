package navigator.trajectory.alternative;

import navigator.Command;
import navigator.user.UserService;

import java.util.Map;


import navigator.user.User;

public class AlternativeCareerController {

    private final UserService userService;
    private final AlternativeCareerService alternativeCareerService;

    public AlternativeCareerController(
            UserService userService,
            AlternativeCareerService alternativeCareerService
    ) {
        this.userService = userService;
        this.alternativeCareerService = alternativeCareerService;
    }


    public Map<String, Command> commands() {

        return Map.of(
                "alternative-professions",
                this::showAlternativeProfessions
        );
    }

    //Показываем альтернативные профессии для текущего авторизованного пользователя
    private void showAlternativeProfessions(String[] args) {

        if (args.length != 0) {
            throw new IllegalArgumentException(
                    "Использование: alternative-professions"
            );
        }

        User user = userService.requireUser();

        alternativeCareerService.printAlternatives(user.id());
    }
}
