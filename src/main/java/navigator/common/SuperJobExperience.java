package org.example.vacancy;

import navigator.common.Grade;

import java.util.List;

public enum SuperJobExperience {

    NO_EXPERIENCE("1", Grade.JUNIOR),
    FROM_ONE_YEAR("2", Grade.JUNIOR),
    FROM_THREE_YEARS("3", Grade.MIDDLE),
    FROM_SIX_YEARS("4", Grade.SENIOR);

    private final String apiValue;
    private final Grade grade;

    SuperJobExperience(String apiValue, Grade grade) {
        this.apiValue = apiValue;
        this.grade = grade;
    }

    public String getApiValue() {
        return apiValue;
    }

    public Grade getGrade() {
        return grade;
    }

    // Получение опыта по идентификатору SuperJob
    public static SuperJobExperience fromApi(String value) {
        for (SuperJobExperience experience : values()) {
            if (experience.apiValue.equals(value)) {
                return experience;
            }
        }

        return null;
    }

    // Получение всех категорий опыта для выбранного уровня
    public static List<SuperJobExperience> forGrade(Grade grade) {
        return List.of(values()).stream()
                .filter(experience -> experience.grade == grade)
                .toList();
    }
}