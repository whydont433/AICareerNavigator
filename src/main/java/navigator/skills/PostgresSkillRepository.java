package navigator.skills;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PostgresSkillRepository implements SkillRepository {

    @Override
    public Long findOrCreateSkill(
            Connection connection,
            NormalizedSkill skill
    ) throws SQLException {

        String sql = """
                INSERT INTO skills (
                    name,
                    category,
                    canonical_name
                )
                VALUES (?, ?, ?)
                ON CONFLICT (canonical_name)
                DO UPDATE SET
                    name = skills.name
                RETURNING id
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, skill.name());
            statement.setString(2, skill.category());
            statement.setString(3, skill.canonicalName());

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return resultSet.getLong("id");
                }
            }
        }

        throw new SQLException(
                "Не удалось получить ID навыка: "
                        + skill.canonicalName()
        );
    }
}
