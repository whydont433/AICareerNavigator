package navigator.vacancy;

import navigator.common.Grade;

// Параметры поиска о вакансиям
public record SearchParams(
        String profession,
        Grade grade,
        int periodDays,
        int limit
) {

    public SearchParams(String profession, Grade grade) {
        this(profession, grade, 30, 250);
    }

    public SearchParams {
        if (profession == null || profession.isBlank()) {
            throw new IllegalArgumentException(
                    "Нужно указать профессию"
            );
        }

        if (grade == null) {
            throw new IllegalArgumentException(
                    "Нужно указать уровень"
            );
        }



        profession = profession.trim();
    }
}


