package navigator.vacancy;

import navigator.common.Grade;

// Параметры поиска о вакансиям
public record SearchParams(
        String profession,
        Grade grade
) {


    public static final int VACANCY_LIMIT = 250;

    public SearchParams {
        if (profession == null || profession.isBlank()) {
            throw new IllegalArgumentException(
            );
        }

        if (grade == null) {
            throw new IllegalArgumentException(
            );
        }

        profession = profession.trim();
    }
}


