package navigator.vacancy;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class MemoryVacancyRepository implements VacancyRepository{
    @Override
    public void save(Vacancy vacancy) {

    }

    @Override
    public List<Vacancy> findAll() {
        return List.of();
    }

    @Override
    public Optional<Vacancy> findBySourceId(String sourceId) {
        return Optional.empty();
    }

    @Override
    public Vacancy save(Vacancy vacancy, String profession) throws SQLException {
        return null;
    }
}
