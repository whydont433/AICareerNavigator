package navigator.vacancy;

import navigator.common.Grade;
import navigator.common.HhExperience;

import java.time.Instant;

public record Vacancy(
        Long id,
        String hhId,
        String name,
        String description,
        Instant publishedAt,
        String url,
        String area,
        String employer,
        HhExperience hhExperience,
        Grade grade,
        Instant fetchedAt,
        Long roleId
){}