package navigator.trajectory.external;

import java.util.List;

/** Справочник навыков реализация — заглушка. */
public interface SkillDictionary {

    /**
     * Найти навык по названию или синониму, без учёта регистра.
     * Пустой список — не найден, один id — найден, несколько — неоднозначно.
     */
    List<Long> find(String text);

    /** Название навыка для вывода: 17 → "Java". */
    String nameOf(long skillId);
}