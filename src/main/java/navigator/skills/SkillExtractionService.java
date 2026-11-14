package navigator.skills;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.gigachat.GigaChatClient;
import navigator.skills.ExtractedSkill;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SkillExtractionService {

    private final GigaChatClient gigaChatClient;
    private final ObjectMapper objectMapper;

    public SkillExtractionService(
            GigaChatClient gigaChatClient
    ) {
        this.gigaChatClient = gigaChatClient;
        this.objectMapper = new ObjectMapper();
    }

    public List<ExtractedSkill> extract(String description)
            throws IOException, InterruptedException {

        String response = gigaChatClient.extractSkills(description);

        String json = cleanJson(response);

        JsonNode root = objectMapper.readTree(json);

        JsonNode skillsNode = root.path("skills");

        if (!skillsNode.isArray()) {
            throw new IOException(
                    "Ответ GigaChat не содержит массива skills"
            );
        }

        List<ExtractedSkill> skills = new ArrayList<>();

        for (JsonNode node : skillsNode) {

            ExtractedSkill skill = objectMapper.treeToValue(
                    node,
                    ExtractedSkill.class
            );

            if (skill.name() == null || skill.name().isBlank()) {
                continue;
            }

            skills.add(skill);
        }

        if (skills.size() > 10) {
            skills = skills.subList(0, 10);
        }

        return skills;
    }

    private String cleanJson(String response) {

        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException(
                    "GigaChat вернул пустой ответ"
            );
        }

        String json = response.trim();

        if (json.startsWith("```json")) {
            json = json.substring(7);
        } else if (json.startsWith("```")) {
            json = json.substring(3);
        }

        if (json.endsWith("```")) {
            json = json.substring(0, json.length() - 3);
        }

        return json.trim();
    }
}
