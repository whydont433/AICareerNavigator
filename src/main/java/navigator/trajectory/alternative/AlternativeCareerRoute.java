package navigator.trajectory.alternative;

import navigator.common.Grade;
import navigator.trajectory.external.RoleInfo;
import java.util.List;


public record AlternativeCareerRoute(RoleInfo profession, Grade grade, double matchPercent,
                                     int sampleSize, List<CareerSkill> strengths, List<CareerSkill> gaps) {
    public AlternativeCareerRoute {
        strengths=List.copyOf(strengths);
        gaps=List.copyOf(gaps);
    }
}
