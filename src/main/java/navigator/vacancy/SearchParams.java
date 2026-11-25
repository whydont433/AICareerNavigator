package navigator.vacancy;

import navigator.common.Grade;

// Параметры поиска о вакансия
public record SearchParams(
        String profession,
        Grade grade
) {


    public static final int VACANCY_LIMIT = readLimit();

    public SearchParams {
        if (profession == null || profession.isBlank()) {
            throw new IllegalArgumentException(
                    "Введите название профессии"
            );
        }

        if (grade == null) {
            throw new IllegalArgumentException(
                    "Не указан уровень: junior, middle или senior"
            );
        }

        profession = profession.trim();
    }

    private static int readLimit() {
        try {
            int value = Integer.parseInt(System.getenv("VACANCY_LIMIT"));
            return value > 0 ? value : 250;
        } catch (NumberFormatException e) {
            return 250;
        }
    }
}


