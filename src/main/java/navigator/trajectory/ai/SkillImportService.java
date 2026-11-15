package navigator.trajectory.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.ai.LlmClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SkillImportService {
    private static final int MAX_TEXT_LENGTH = 3000;
    private static final Map<Integer, String> LEVEL_SCALE = Map.of(
            0, "не знаком",
            1, "читал, понимаю зачем нужна, почти не использовал",
            2, "решал учебные задачи, изучал документацию",
            3, "делал пет-проект",
            4, "применял в реальном проекте с подсказками",
            5, "уверенно применяю в работе",
            6, "проектирую и оптимизирую"
    );

    private final LlmClient llmClient;
    private final ObjectMapper mapper;

    public SkillImportService(LlmClient llmClient) {
        this.llmClient = llmClient;
        this.mapper = new ObjectMapper();
    }

    public List<ImportedSkill> extractSkills(String rawText, Map<Long, String> dictionarySkills) {
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("Текст о себе пуст");
        }
        String text = rawText.length() > MAX_TEXT_LENGTH ? rawText.substring(0, MAX_TEXT_LENGTH) : rawText;

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String payload = mapper.writeValueAsString(Map.of(
                        "text", text,
                        "skills", dictionarySkills,
                        "scale", LEVEL_SCALE
                ));
                String rawJson = llmClient.completeJson("skill_import.txt", payload);
                return parseAndValidate(rawJson, text, dictionarySkills);
            } catch (Exception ignored) {
            }
        }
        throw new IllegalStateException("Не удалось разобрать текст, введите данные вручную");
    }

    public List<ImportedSkill> parseAndValidate(String rawJson, String sourceText, Map<Long, String> dictionarySkills) {
        try {
            String cleaned = stripCodeFence(rawJson);
            JsonNode array = mapper.readTree(cleaned);
            if (!array.isArray()) {
                throw new IllegalArgumentException("Ожидался JSON-массив");
            }
            Map<Long, ImportedSkill> validated = new LinkedHashMap<>();
            for (JsonNode item : array) {
                if (!item.has("skill_id") || !item.has("level") || !item.has("evidence")) {
                    continue;
                }
                long skillId = item.get("skill_id").asLong(-1);
                int level = item.get("level").asInt(-1);
                String evidence = item.get("evidence").asText("");

                if (!dictionarySkills.containsKey(skillId)) {
                    continue;
                }
                if (level < 0 || level > 6) {
                    continue;
                }
                if (evidence.isBlank() || !sourceText.contains(evidence)) {
                    continue;
                }
                validated.put(skillId, new ImportedSkill(skillId, dictionarySkills.get(skillId), level, evidence));
            }
            return new ArrayList<>(validated.values());
        } catch (Exception e) {
            throw new IllegalArgumentException("Невалидный JSON от ИИ", e);
        }
    }

    private String stripCodeFence(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    public record ImportedSkill(long skillId, String skillName, int level, String evidence) {
    }
}
