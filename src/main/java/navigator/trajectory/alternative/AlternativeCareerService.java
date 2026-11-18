
package navigator.trajectory.alternative;

import navigator.common.Grade;
import navigator.trajectory.api.MatchResult;
import navigator.trajectory.api.RoleProfile;
import navigator.trajectory.api.UserSkill;
import navigator.trajectory.api.UserTarget;
import navigator.trajectory.matching.MatchingService;
import navigator.trajectory.profile.UserSkillRepository;
import navigator.trajectory.profile.UserTargetRepository;
import navigator.trajectory.role.RoleProfileRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//Подбор альтернативных профессий на основе навыков пользователя и профилей профессий
public class AlternativeCareerService {

    private static final int RESULT_LIMIT = 5;

    private UserSkillRepository userSkillRepository;
    private RoleProfileRepository roleProfileRepository;
    private UserTargetRepository userTargetRepository;

    private final MatchingService matchingService;

    public AlternativeCareerService(
            ) {
        this.userSkillRepository = userSkillRepository;
        this.roleProfileRepository = roleProfileRepository;
        this.userTargetRepository = userTargetRepository;

        this.matchingService = new MatchingService(
                roleProfileRepository,
                userSkillRepository
        );
    }

    //Подбирает и выводит пять наиболее подходящих альтернативных профессий
    public void printAlternatives(long userId) {

        //Получаем выбранную профессию пользователя из БД
        UserTarget target = userTargetRepository.findTarget(userId).orElse(null);

        if (target == null) {
            System.out.println("Для пользователя не задана основная профессия.");
            return;
        }

        // Получаем навыки пользователя из БД
        List<UserSkill> userSkills = userSkillRepository.findAll(userId);

        if (userSkills.isEmpty()) {
            System.out.println("У пользователя пока нет сохранённых навыков.");
            return;
        }

        //Получаем профили всех профессий из БД
        List<RoleProfile> allProfiles = roleProfileRepository.findAll();

        // Исключаем основную профессию пользователя.
        List<RoleProfile> profiles = allProfiles.stream()
                .filter(profile -> !profile.roleId().equals(target.roleId()))
                .toList();

        if (profiles.isEmpty()) {
            System.out.println("Не найдены профили других профессий для сравнения.");
            return;
        }

        //Используем существующий механизм сравнения
        MatchingService matchingService = new MatchingService(roleProfileRepository, userSkillRepository);

        // Для каждой профессии выбираем наиболее подходящий пользователю грейд
        Map<String, Alternative> bestByProfession = new LinkedHashMap<>();

        for (RoleProfile profile : profiles) {

            MatchResult match =
                    matchingService.calculate(profile, userSkills);

            Alternative candidate = new Alternative(
                    profile.roleId(),
                    profile.grade(),
                    match.matchPercent(),
                    profile.sampleSize(),
                    match.strengths().size(),
                    match.gaps().size()
            );

            Alternative previous = bestByProfession.get(profile.roleId());

            if (previous == null || isBetter(candidate, previous)) {

                bestByProfession.put(
                        profile.roleId(),
                        candidate
                );
            }
        }

        // Сортируем профессии по проценту совпадения
        List<Alternative> alternatives = new ArrayList<>(bestByProfession.values());

        alternatives.sort(
                Comparator
                        .comparingDouble(
                                Alternative::matchPercent
                        )
                        .reversed()
                        .thenComparing(
                                Comparator.comparingInt(
                                        Alternative::sampleSize
                                ).reversed()
                        )
                        .thenComparing(
                                Alternative::professionName,
                                String.CASE_INSENSITIVE_ORDER
                        )
        );

        // Выводим результат
        System.out.println();

        System.out.println("Альтернативные профессии для основной профессии: " + target.roleId());

        System.out.println(
                "Учитывается навыков пользователя: " + userSkills.size());

        System.out.println(
                "--------------------------------------------------"
        );

        int count = Math.min(RESULT_LIMIT, alternatives.size());

        for (int i = 0; i < count; i++) {

            Alternative alternative = alternatives.get(i);

            System.out.printf(
                    "%d. %s | %s | совпадение: %.1f%% | вакансий в профиле: %d%n",
                    i + 1,
                    alternative.professionName(),
                    alternative.grade(),
                    alternative.matchPercent(),
                    alternative.sampleSize()
            );

            System.out.printf(
                    "   Подходящих навыков: %d, навыков для развития: %d%n",
                    alternative.strengthsCount(),
                    alternative.gapsCount()
            );
        }
    }

    /**
     * Определяем, какой грейд одной профессии лучше подходит.
     *
     * Приоритет:
     * 1. Более высокий процент совпадения.
     * 2. Более высокий грейд.
     * 3. Большее количество вакансий в профиле.
     */
    private boolean isBetter(
            Alternative candidate,
            Alternative previous
    ) {

        int byMatch = Double.compare(
                candidate.matchPercent(),
                previous.matchPercent()
        );

        if (byMatch != 0) {
            return byMatch > 0;
        }

        int byGrade = Integer.compare(
                gradeRank(candidate.grade()),
                gradeRank(previous.grade())
        );

        if (byGrade != 0) {
            return byGrade > 0;
        }

        return candidate.sampleSize() > previous.sampleSize();
    }

    private int gradeRank(Grade grade) {

        return switch (grade) {
            case JUNIOR -> 1;
            case MIDDLE -> 2;
            case SENIOR -> 3;
        };
    }

    private record Alternative(
            String professionName,
            Grade grade,
            double matchPercent,
            int sampleSize,
            int strengthsCount,
            int gapsCount
    ) { }
}
