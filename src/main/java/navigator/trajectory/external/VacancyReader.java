package navigator.trajectory.external;

import navigator.common.Grade;

import java.util.List;

/** Вакансии с навыками. Реализация — заглушка. */
public interface VacancyReader {

    /** Все вакансии роли и грейда. */
    List<VacancyData> find(String roleId, Grade grade);
}