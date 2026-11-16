package navigator.vacancy;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;


public interface VacancyRepository {

    // Сохранить вакансию
    void save(Vacancy vacancy);

    // Получить все вакансии
    List<Vacancy> findAll();

    // Найти вакансию по ID с SuperJob
    Optional<Vacancy> findBySourceId(String sourceId);
    Vacancy save(Vacancy vacancy, String profession)
            throws SQLException;
}