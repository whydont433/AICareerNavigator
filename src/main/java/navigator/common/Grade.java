package navigator.common;

public enum Grade {
    JUNIOR,
    MIDDLE,
    SENIOR;

    public static Grade parse(String text) {
        for (Grade g : values()) {
            if (g.name().equalsIgnoreCase(text)) {
                return g;
            }
        }
        throw new IllegalArgumentException("Грейд: junior, middle или senior");
    }
}
