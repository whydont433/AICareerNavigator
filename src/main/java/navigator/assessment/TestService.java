package navigator.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import navigator.ai.LlmClient;
import navigator.common.Grade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class TestService {
    public static final int QUESTIONS_PER_LEVEL = 3;
    public static final int PASS_THRESHOLD = 2;

    private final TestRepository repository;
    private final LlmClient llmClient;
    private final ObjectMapper mapper;
    private final List<Consumer<TestCompleted>> listeners = new ArrayList<>();

    public TestService(TestRepository repository, LlmClient llmClient) {
        this.repository = repository;
        this.llmClient = llmClient;
        this.mapper = new ObjectMapper();
    }

    public void onTestCompleted(Consumer<TestCompleted> listener) {
        listeners.add(listener);
    }

    public int resolveTargetLevel(Integer roleRequiredLevel, Integer userCurrentLevel) {
        if (roleRequiredLevel != null) {
            return roleRequiredLevel;
        }
        if (userCurrentLevel != null && userCurrentLevel > 0) {
            return userCurrentLevel;
        }
        return 2;
    }

    public int[] calculateWindow(int targetLevel) {
        int clamped = Math.max(1, Math.min(6, targetLevel));
        int from = Math.max(1, Math.min(4, clamped - 1));
        int to = from + 2;
        return new int[]{from, to};
    }

    public int calculateTestLevel(int levelFrom, List<Integer> correctPerLevel) {
        int passedStreak = 0;
        for (int correct : correctPerLevel) {
            if (correct >= PASS_THRESHOLD) {
                passedStreak++;
            } else {
                break;
            }
        }
        return (levelFrom - 1) + passedStreak;
    }

    public List<TestQuestion> prepareQuestions(long userId, long skillId, String skillName,
                                               String roleName, Grade grade, int targetLevel) {
        int[] window = calculateWindow(targetLevel);
        Map<Integer, List<TestQuestion>> byLevel = new LinkedHashMap<>();
        Set<Integer> missingLevels = new HashSet<>();

        for (int lvl = window[0]; lvl <= window[1]; lvl++) {
            List<TestQuestion> unseen = new ArrayList<>(repository.findUnseenQuestions(userId, skillId, lvl, QUESTIONS_PER_LEVEL));
            byLevel.put(lvl, unseen);
            if (unseen.size() < QUESTIONS_PER_LEVEL) {
                missingLevels.add(lvl);
            }
        }

        if (!missingLevels.isEmpty()) {
            List<String> existingTexts = repository.findAllQuestionTexts(skillId, 20);
            Set<String> knownTexts = new HashSet<>(existingTexts);
            List<TestQuestion> generated = tryGenerateWithAi(skillId, skillName, roleName, grade, missingLevels, existingTexts, knownTexts);
            if (!generated.isEmpty()) {
                List<TestQuestion> saved = repository.saveAiQuestions(skillId, generated);
                for (TestQuestion q : saved) {
                    List<TestQuestion> bucket = byLevel.get(q.level());
                    if (bucket != null && bucket.size() < QUESTIONS_PER_LEVEL) {
                        bucket.add(q);
                    }
                }
            }
        }

        List<TestQuestion> result = new ArrayList<>();
        for (int lvl = window[0]; lvl <= window[1]; lvl++) {
            List<TestQuestion> bucket = byLevel.get(lvl);
            if (bucket.size() < QUESTIONS_PER_LEVEL) {
                Set<Long> alreadyIds = new HashSet<>();
                for (TestQuestion q : bucket) {
                    alreadyIds.add(q.id());
                }
                for (TestQuestion fallback : repository.findAnyQuestions(skillId, lvl, QUESTIONS_PER_LEVEL)) {
                    if (bucket.size() >= QUESTIONS_PER_LEVEL) {
                        break;
                    }
                    if (!alreadyIds.contains(fallback.id())) {
                        bucket.add(fallback);
                        alreadyIds.add(fallback.id());
                    }
                }
            }
            if (bucket.size() < QUESTIONS_PER_LEVEL) {
                throw new IllegalStateException("Не удалось подготовить тест по навыку, попробуйте позже");
            }
            result.addAll(bucket.subList(0, QUESTIONS_PER_LEVEL));
        }
        return result;
    }

    private List<TestQuestion> tryGenerateWithAi(long skillId, String skillName, String roleName,
                                                 Grade grade, Set<Integer> levels,
                                                 List<String> existingTexts, Set<String> knownTexts) {
        if (llmClient == null) {
            return List.of();
        }
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                String payload = mapper.writeValueAsString(Map.of(
                        "skill", skillName,
                        "role", roleName == null ? "" : roleName,
                        "grade", grade == null ? "" : grade.name(),
                        "levels", levels,
                        "existingQuestions", existingTexts
                ));
                String json = llmClient.completeJson("test_generation.txt", payload);
                List<TestQuestion> parsed = parseAndValidateAiQuestions(skillId, json, levels, knownTexts);
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            } catch (Exception ignored) {
            }
        }
        return List.of();
    }

    public List<TestQuestion> parseAndValidateAiQuestions(long skillId, String rawJson,
                                                          Set<Integer> allowedLevels, Set<String> knownTexts) {
        try {
            String cleaned = stripCodeFence(rawJson);
            JsonNode array = mapper.readTree(cleaned);
            if (!array.isArray()) {
                return List.of();
            }
            List<TestQuestion> valid = new ArrayList<>();
            Set<String> seenInBatch = new HashSet<>(knownTexts);

            for (JsonNode item : array) {
                int level = item.path("level").asInt(-1);
                String question = item.path("question").asText("").trim();
                int correctIndex = item.path("correct_index").asInt(-1);
                String explanation = item.path("explanation").asText("").trim();
                String topic = item.path("topic").asText("").trim();
                JsonNode optsNode = item.path("options");

                if (!allowedLevels.contains(level)) {
                    continue;
                }
                if (question.isEmpty() || !seenInBatch.add(question)) {
                    continue;
                }
                if (correctIndex < 0 || correctIndex > 3) {
                    continue;
                }
                if (explanation.isEmpty() || topic.isEmpty()) {
                    continue;
                }
                if (!optsNode.isArray() || optsNode.size() != 4) {
                    continue;
                }
                List<String> options = new ArrayList<>();
                Set<String> uniqueOptions = new HashSet<>();
                boolean badOption = false;
                for (JsonNode opt : optsNode) {
                    String text = opt.asText("").trim();
                    if (text.isEmpty() || !uniqueOptions.add(text)) {
                        badOption = true;
                        break;
                    }
                    options.add(text);
                }
                if (badOption) {
                    continue;
                }
                valid.add(new TestQuestion(0, skillId, level, question, options, correctIndex, explanation, topic));
            }
            return valid;
        } catch (Exception e) {
            return List.of();
        }
    }

    public TestResult completeTest(long attemptId, long userId, long skillId, int requiredLevel,
                                   int levelFrom, List<TestQuestion> questions, List<Integer> chosenOptions) {
        return completeTest(attemptId, userId, skillId, requiredLevel, levelFrom, questions, chosenOptions, r -> {});
    }

    public TestResult completeTest(long attemptId, long userId, long skillId, int requiredLevel,
                                   int levelFrom, List<TestQuestion> questions, List<Integer> chosenOptions,
                                   Consumer<TestResult> onResultReady) {
        Map<Integer, Integer> correctByLevel = new LinkedHashMap<>();
        for (int i = 0; i < 3; i++) {
            correctByLevel.put(levelFrom + i, 0);
        }

        int totalCorrect = 0;
        List<TestResult.WrongAnswer> wrongAnswers = new ArrayList<>();
        Map<String, Integer> topicErrors = new HashMap<>();

        for (int i = 0; i < questions.size(); i++) {
            TestQuestion q = questions.get(i);
            Integer chosen = chosenOptions.get(i);
            boolean isCorrect = chosen != null && chosen == q.correctIndex();
            repository.saveAnswer(attemptId, q.id(), chosen, isCorrect);

            if (isCorrect) {
                totalCorrect++;
                correctByLevel.merge(q.level(), 1, Integer::sum);
            } else {
                String chosenText = (chosen != null && chosen >= 0 && chosen < q.options().size())
                        ? q.options().get(chosen)
                        : "пропуск";
                wrongAnswers.add(new TestResult.WrongAnswer(
                        q.question(),
                        chosenText,
                        q.options().get(q.correctIndex()),
                        q.explanation()
                ));
                topicErrors.merge(q.topic(), 1, Integer::sum);
            }
        }

        List<Integer> streakInput = new ArrayList<>(correctByLevel.values());
        int testLevel = calculateTestLevel(levelFrom, streakInput);
        int totalQuestions = questions.size();
        repository.completeAttempt(attemptId, totalCorrect, totalQuestions, testLevel);

        Map<String, Integer> sortedTopics = new LinkedHashMap<>();
        topicErrors.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(e -> sortedTopics.put(e.getKey(), e.getValue()));

        TestCompleted event = new TestCompleted(userId, skillId, testLevel, totalCorrect, totalQuestions);
        TestResult result = new TestResult(skillId, requiredLevel, testLevel, totalCorrect, totalQuestions,
                correctByLevel, wrongAnswers, sortedTopics, event);

        onResultReady.accept(result);

        for (Consumer<TestCompleted> listener : listeners) {
            listener.accept(event);
        }

        return result;
    }

    public void abandonTest(long attemptId) {
        repository.abandonAttempt(attemptId);
    }

    public long startAttempt(long userId, long skillId, int targetLevel, int levelFrom, int levelTo) {
        return repository.createAttempt(userId, skillId, targetLevel, levelFrom, levelTo);
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
}
