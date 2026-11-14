package navigator.vacancy;

import navigator.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class PostgresVacancyRepository implements VacancyRepository {

    private final DatabaseConfig databaseConfig;

    public PostgresVacancyRepository(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

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
    public Vacancy save(Vacancy vacancy, String profession)
            throws SQLException {

        try (Connection connection = databaseConfig.getConnection()) {

            connection.setAutoCommit(false);

            try {
                long professionId = getOrCreateProfession(connection, profession);

                long vacancyId = saveVacancy(connection, vacancy, professionId);

                connection.commit();

                return vacancy.withId(vacancyId);

            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }


    private long getOrCreateProfession(Connection connection, String profession) throws SQLException {

        String sql = """
                INSERT INTO professions (name)
                VALUES (?)
                ON CONFLICT (name)
                DO UPDATE SET name = EXCLUDED.name
                RETURNING id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, profession.trim());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }

                throw new SQLException(
                        "Не удалось получить ID профессии"
                );
            }
        }
    }

    //Сохраняем вакансию или обновляем существующую
    private long saveVacancy(
            Connection connection,
            Vacancy vacancy,
            long professionId
    ) throws SQLException {

        String sql = """
                INSERT INTO vacancies (
                    source_id,
                    profession_id,
                    name,
                    description,
                    published_at,
                    url,
                    area,
                    employer,
                    superjob_experience,
                    grade,
                    fetched_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (source_id)
                DO UPDATE SET
                    profession_id = EXCLUDED.profession_id,
                    name = EXCLUDED.name,
                    description = EXCLUDED.description,
                    published_at = EXCLUDED.published_at,
                    url = EXCLUDED.url,
                    area = EXCLUDED.area,
                    employer = EXCLUDED.employer,
                    superjob_experience = EXCLUDED.superjob_experience,
                    grade = EXCLUDED.grade,
                    fetched_at = EXCLUDED.fetched_at
                RETURNING id
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, vacancy.sourceId());
            statement.setLong(2, professionId);
            statement.setString(3, vacancy.name());
            statement.setString(4, vacancy.description());

            setTimestamp(statement, 5, vacancy.publishedAt());

            statement.setString(6, vacancy.url());
            statement.setString(7, vacancy.area());
            statement.setString(8, vacancy.employer());

            statement.setString(9, vacancy.experience() == null ? null : vacancy.experience().name());

            statement.setString(10, vacancy.grade().name());

            setTimestamp(statement, 11, vacancy.fetchedAt());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }

                throw new SQLException(
                        "Не удалось сохранить вакансию"
                );
            }
        }
    }

    private void setTimestamp(PreparedStatement statement, int index, java.time.Instant instant) throws SQLException {

        if (instant == null) {
            statement.setTimestamp(index, null);
        } else {
            statement.setTimestamp(index, Timestamp.from(instant));
        }
    }
}
