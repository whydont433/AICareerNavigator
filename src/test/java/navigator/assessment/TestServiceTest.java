package navigator.assessment;

import navigator.common.Grade;
import navigator.trajectory.ai.SkillImportService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestServiceTest {

    @Test
    void windowBoundariesMatchSpecification() {
        TestService service = new TestService(new InMemoryTestRepository(), null);

        assertEquals(List.of(1, 3), toList(service.calculateWindow(1)));
        assertEquals(List.of(3, 5), toList(service.calculateWindow(4)));
        assertEquals(List.of(4, 6), toList(service.calculateWindow(6)));

        int extraTarget = service.resolveTargetLevel(null, 0);
        assertEquals(2, extraTarget);
        assertEquals(List.of(1, 3), toList(service.calculateWindow(extraTarget)));
    }

    @Test
    void testLevelCalculatedByConsecutivePassedLevels() {
        TestService service = new TestService(new InMemoryTestRepository(), null);

        assertEquals(4, service.calculateTestLevel(3, List.of(3, 2, 1)));
        assertEquals(2, service.calculateTestLevel(3, List.of(1, 3, 3)));
        assertEquals(5, service.calculateTestLevel(3, List.of(2, 2, 3)));
        assertEquals(0, service.calculateTestLevel(1, List.of(1, 3, 3)));
    }

    @Test
    void selectsUnseenBeforeSeenAndCallsAiOnlyWhenShort() {
        InMemoryTestRepository repo = new InMemoryTestRepository();
        for (int lvl = 1; lvl <= 3; lvl++) {
            for (int i = 1; i <= 3; i++) {
                repo.bank.add(new TestQuestion(lvl * 10L + i, 1L, lvl,
                        "Q" + lvl + "_" + i, List.of("A", "B", "C", "D"), 0, "Exp", "Topic"));
            }
        }
        AtomicInteger llmCalls = new AtomicInteger(0);
        TestService service = new TestService(repo, (prompt, payload) -> {
            llmCalls.incrementAndGet();
            return "[]";
        });

        List<TestQuestion> selected = service.prepareQuestions(1L, 1L, "Java", "Java Developer", Grade.JUNIOR, 2);
        assertEquals(9, selected.size());
        assertEquals(0, llmCalls.get());
    }

    @Test
    void invalidAiQuestionsFilteredAndFallbackUsedAfterTwoFailures() {
        InMemoryTestRepository repo = new InMemoryTestRepository();
        for (int lvl = 1; lvl <= 3; lvl++) {
            for (int i = 1; i <= 3; i++) {
                TestQuestion q = new TestQuestion(lvl * 10L + i, 1L, lvl,
                        "SeenQ" + lvl + "_" + i, List.of("A", "B", "C", "D"), 0, "Exp", "Topic");
                repo.bank.add(q);
                repo.seenIds.add(q.id());
            }
        }
        AtomicInteger llmCalls = new AtomicInteger(0);
        TestService service = new TestService(repo, (prompt, payload) -> {
            llmCalls.incrementAndGet();
            return "not-a-json";
        });

        List<TestQuestion> fallback = service.prepareQuestions(1L, 1L, "Java", "Java Developer", Grade.JUNIOR, 1);
        assertEquals(9, fallback.size());
        assertEquals(2, llmCalls.get());

        String mixedJson = """
                [
                  {"level": 1, "question": "Valid", "options": ["A","B","C","D"], "correct_index": 2, "explanation": "Ok", "topic": "Core"},
                  {"level": 1, "question": "ThreeOpts", "options": ["A","B","C"], "correct_index": 1, "explanation": "No", "topic": "Core"},
                  {"level": 1, "question": "BadIdx", "options": ["A","B","C","D"], "correct_index": 4, "explanation": "No", "topic": "Core"},
                  {"level": 5, "question": "WrongLvl", "options": ["A","B","C","D"], "correct_index": 1, "explanation": "No", "topic": "Core"},
                  {"level": 1, "question": "Valid", "options": ["A","B","C","D"], "correct_index": 2, "explanation": "Dup", "topic": "Core"}
                ]
                """;
        List<TestQuestion> validated = service.parseAndValidateAiQuestions(1L, mixedJson, Set.of(1, 2, 3), Set.of());
        assertEquals(1, validated.size());
        assertEquals("Valid", validated.get(0).question());
    }

    @Test
    void publishesSingleEventOnCompleteAndNoneOnAbandon() {
        InMemoryTestRepository repo = new InMemoryTestRepository();
        TestService service = new TestService(repo, null);
        List<TestCompleted> events = new ArrayList<>();
        service.onTestCompleted(events::add);

        List<TestQuestion> questions = new ArrayList<>();
        List<Integer> answers = new ArrayList<>();
        for (int lvl = 3; lvl <= 5; lvl++) {
            for (int i = 0; i < 3; i++) {
                questions.add(new TestQuestion(lvl * 10L + i, 1L, lvl, "Q", List.of("A", "B", "C", "D"), 1, "E", "T"));
                answers.add(lvl == 3 && i == 2 ? null : (lvl <= 4 ? 1 : 0));
            }
        }

        long attemptId = service.startAttempt(7L, 1L, 4, 3, 5);
        TestResult result = service.completeTest(attemptId, 7L, 1L, 4, 3, questions, answers);

        assertEquals(1, events.size());
        assertEquals(4, result.testLevel());
        assertEquals(5, result.correct());
        assertEquals(9, result.total());

        long abandonedId = service.startAttempt(7L, 1L, 4, 3, 5);
        service.abandonTest(abandonedId);
        assertEquals(1, events.size());
    }

    @Test
    void skillImportRejectsHallucinations() {
        SkillImportService importService = new SkillImportService((p, u) -> """
                [
                  {"skill_id": 1, "level": 3, "evidence": "писал пет-проект на Java"},
                  {"skill_id": 999, "level": 4, "evidence": "писал пет-проект на Java"},
                  {"skill_id": 2, "level": 9, "evidence": "писал пет-проект на Java"},
                  {"skill_id": 3, "level": 3, "evidence": "несуществующая цитата"}
                ]
                """);

        Map<Long, String> dict = Map.of(1L, "Java", 2L, "SQL", 3L, "Spring");
        List<SkillImportService.ImportedSkill> res = importService.extractSkills("Я писал пет-проект на Java полгода", dict);

        assertEquals(1, res.size());
        assertEquals(1L, res.get(0).skillId());
        assertEquals(3, res.get(0).level());

        SkillImportService brokenService = new SkillImportService((p, u) -> "invalid-json");
        assertThrows(IllegalStateException.class, () -> brokenService.extractSkills("Текст", dict));
    }

    private List<Integer> toList(int[] arr) {
        return List.of(arr[0], arr[1]);
    }

    private static class InMemoryTestRepository implements TestRepository {
        final List<TestQuestion> bank = new ArrayList<>();
        final List<Long> seenIds = new ArrayList<>();
        final Map<Long, String> statuses = new HashMap<>();
        long nextId = 1;

        @Override
        public List<TestQuestion> findUnseenQuestions(long userId, long skillId, int level, int limit) {
            return bank.stream()
                    .filter(q -> q.skillId() == skillId && q.level() == level && !seenIds.contains(q.id()))
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<TestQuestion> findAnyQuestions(long skillId, int level, int limit) {
            return bank.stream()
                    .filter(q -> q.skillId() == skillId && q.level() == level)
                    .limit(limit)
                    .toList();
        }

        @Override
        public List<String> findAllQuestionTexts(long skillId, int limit) {
            return bank.stream().map(TestQuestion::question).limit(limit).toList();
        }

        @Override
        public List<TestQuestion> saveAiQuestions(long skillId, List<TestQuestion> questions) {
            return questions;
        }

        @Override
        public long createAttempt(long userId, long skillId, int targetLevel, int levelFrom, int levelTo) {
            long id = nextId++;
            statuses.put(id, "in_progress");
            return id;
        }

        @Override
        public void saveAnswer(long attemptId, long questionId, Integer chosenOption, boolean isCorrect) {
        }

        @Override
        public void completeAttempt(long attemptId, int correct, int total, int testLevel) {
            statuses.put(attemptId, "completed");
        }

        @Override
        public void abandonAttempt(long attemptId) {
            statuses.put(attemptId, "abandoned");
        }

        @Override
        public Optional<Integer> findLastTestLevel(long userId, long skillId) {
            return Optional.empty();
        }
    }
}
