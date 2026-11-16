package navigator.analysis;

import navigator.gigachat.GigaChatClient;
import navigator.skills.*;

public class VacancyAnalysisController {

    public static void main(String[] args) {

        try {

            // 1. Клиент GigaChat
            GigaChatClient gigaChatClient =
                    new GigaChatClient();

            // 2. Извлечение навыков
            SkillExtractionService extractionService =
                    new SkillExtractionService(gigaChatClient);

            // 3. Словарь синонимов
            SkillSynonymDictionary synonymDictionary =
                    new SkillSynonymDictionary();

            // 4. Нормализация
            SkillNormalizationService normalizationService =
                    new SkillNormalizationService(
                            synonymDictionary
                    );

            // 5. Репозитории
            SkillRepository skillRepository =
                    new PostgresSkillRepository();

            VacancyAnalysisRepository vacancyRepository =
                    new PostgresVacancyAnalysisRepository(
                            skillRepository
                    );

            // 6. Основной сервис
            VacancyAnalysisService analysisService =
                    new VacancyAnalysisService(
                            vacancyRepository,
                            extractionService,
                            normalizationService
                    );

            // 7. Запуск
            analysisService.analyzeVacancies();

        } catch (Exception e) {

            System.err.println(
                    "Ошибка запуска анализа вакансий"
            );

            e.printStackTrace();
        }
    }
}