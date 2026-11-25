package navigator.analysis;

import navigator.common.Grade;
import navigator.skills.ExtractedSkill;
import navigator.skills.NormalizedSkill;
import navigator.skills.SkillExtractionService;
import navigator.skills.SkillNormalizationService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VacancyAnalysisService {

    private static final int BATCH_SIZE = 20;

    private final VacancyAnalysisRepository repository;
    private final SkillExtractionService extractionService;
    private final SkillNormalizationService normalizationService;

    public VacancyAnalysisService(
            VacancyAnalysisRepository repository,
            SkillExtractionService extractionService,
            SkillNormalizationService normalizationService
    ) {
        this.repository = repository;
        this.extractionService = extractionService;
        this.normalizationService = normalizationService;
    }

    public void analyzeVacancies() throws SQLException {

        List<VacancyForAnalysis> vacancies =
                repository.findUnanalyzedVacancies(BATCH_SIZE);

        System.out.println(
                "Найдено необработанных вакансий: "
                        + vacancies.size()
        );

        int success = 0;
        int failed = 0;

        for (VacancyForAnalysis vacancy : vacancies) {

            System.out.println(
                    "Обрабатываем вакансию: "
                            + vacancy.name()
            );

            try {

                List<ExtractedSkill> extractedSkills =
                        extractionService.extract(
                                vacancy.description()
                        );

                List<NormalizedSkill> normalizedSkills =
                        normalizeSkills(extractedSkills);

                repository.saveVacancySkills(
                        vacancy.id(),
                        normalizedSkills
                );

                success++;

                System.out.println(
                        "Успешно обработана вакансия "
                                + vacancy.id()
                                + ". Найдено навыков: "
                                + normalizedSkills.size()
                );

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                System.err.println(
                        "Обработка прервана"
                );

                break;

            } catch (Exception e) {

                failed++;

                System.err.println(
                        "Ошибка обработки вакансии "
                                + vacancy.id()
                );

                e.printStackTrace();
            }
        }

        System.out.println("Обработка завершена.");
        System.out.println("Успешно: " + success);
        System.out.println("Ошибок: " + failed);
    }


    public record Summary(int total, int success, int failed) {
    }

    public Summary analyzeVacancies(long professionId, Grade grade)
            throws SQLException, InterruptedException {

        final int maxConsecutiveFailures = 3;

        int total = 0;
        int success = 0;
        int failed = 0;
        int consecutiveFailures = 0;
        String lastError = null;
        long lastId = 0;

        while (true) {

            List<VacancyForAnalysis> batch =
                    repository.findUnanalyzedVacancies(
                            professionId, grade, lastId, BATCH_SIZE
                    );

            if (batch.isEmpty()) {
                break;
            }

            for (VacancyForAnalysis vacancy : batch) {

                lastId = Math.max(lastId, vacancy.id());
                total++;

                try {

                    List<ExtractedSkill> extractedSkills =
                            extractionService.extract(vacancy.description());

                    List<NormalizedSkill> normalizedSkills =
                            normalizeSkills(extractedSkills);

                    repository.saveVacancySkills(
                            vacancy.id(),
                            normalizedSkills
                    );

                    success++;
                    consecutiveFailures = 0;

                } catch (IllegalStateException e) {

                    throw new IllegalStateException(
                            "GigaChat недоступен: " + e.getMessage(), e
                    );

                } catch (java.io.IOException
                         | IllegalArgumentException e) {

                    failed++;
                    consecutiveFailures++;
                    lastError = e.getMessage();

                    if (consecutiveFailures >= maxConsecutiveFailures) {
                        throw new IllegalStateException(
                                "GigaChat недоступен: " + maxConsecutiveFailures
                                        + " ошибки подряд. Последняя: "
                                        + lastError, e
                        );
                    }
                }
            }
        }

        return new Summary(total, success, failed);
    }

    private List<NormalizedSkill> normalizeSkills(
            List<ExtractedSkill> extractedSkills
    ) {

        List<NormalizedSkill> result = new ArrayList<>();

        for (ExtractedSkill skill : extractedSkills) {

            try {

                NormalizedSkill normalized =
                        normalizationService.normalize(skill);

                result.add(normalized);

            } catch (IllegalArgumentException e) {
            }
        }

        return result;
    }
}
