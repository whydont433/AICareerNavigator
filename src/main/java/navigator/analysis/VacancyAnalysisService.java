package navigator.analysis;

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

                System.err.println(
                        "Пропущен некорректный навык: "
                                + skill.name()
                                + ". Причина: "
                                + e.getMessage()
                );
            }
        }

        return result;
    }
}
