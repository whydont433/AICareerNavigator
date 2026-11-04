package navigator.vacancy;

import navigator.common.Grade;

import java.time.Instant;

public record Vacancy(
        Long id,
        String sourceId,
        String name,
        String description,
        Instant publishedAt,
        String url,
        String area,
        String employer,
        org.example.vacancy.SuperJobExperience experience,
        Grade grade,
        Instant fetchedAt,
        Long roleId
) {


}