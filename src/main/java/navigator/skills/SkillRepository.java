package navigator.skills;

import java.sql.Connection;
import java.sql.SQLException;

public interface SkillRepository {

    Long findOrCreateSkill(
            Connection connection,
            NormalizedSkill skill
    ) throws SQLException;
}
