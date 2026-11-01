package navigator.trajectory.api;

/** Источник уровня навыка пользователя. **/
public enum SkillSource {
    MANUAL,   // ввёл сам
    AI,       // извлечён из текста
    TEST      // по результату теста
}