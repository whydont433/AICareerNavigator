package navigator.common;

public enum HhExperience {

    NO_EXPERIENCE("noExperience", Grade.JUNIOR),
    BETWEEN_1_AND_3("between1And3", Grade.JUNIOR),
    BETWEEN_3_AND_6("between3And6", Grade.MIDDLE),
    MORE_THAN_6("moreThan6", Grade.SENIOR);

    private final String apiValue;
    private final Grade grade;

    HhExperience(String apiValue, Grade grade) {
        this.apiValue = apiValue;
        this.grade = grade;
    }

    public String getApiValue() {
        return apiValue;
    }

    public Grade getGrade() {
        return grade;
    }



    public static HhExperience fromApi(String value) {
        for (HhExperience experience : values()) {
            if (experience.apiValue.equals(value)) {
                return experience;
            }
        }

        return null;
    }
}