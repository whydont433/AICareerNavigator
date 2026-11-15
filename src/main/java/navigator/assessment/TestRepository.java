package navigator.assessment;

import java.util.List;
import java.util.Optional;

public interface TestRepository {
    List<TestQuestion> findUnseenQuestions(long userId, long skillId, int level, int limit);

    List<TestQuestion> findAnyQuestions(long skillId, int level, int limit);

    List<String> findAllQuestionTexts(long skillId, int limit);

    List<TestQuestion> saveAiQuestions(long skillId, List<TestQuestion> questions);

    long createAttempt(long userId, long skillId, int targetLevel, int levelFrom, int levelTo);

    void saveAnswer(long attemptId, long questionId, Integer chosenOption, boolean isCorrect);

    void completeAttempt(long attemptId, int correct, int total, int testLevel);

    void abandonAttempt(long attemptId);

    Optional<Integer> findLastTestLevel(long userId, long skillId);
}
