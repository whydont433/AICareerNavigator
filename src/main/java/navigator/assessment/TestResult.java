package navigator.assessment;

import java.util.List;
import java.util.Map;

public record TestResult(
        long skillId,
        int requiredLevel,
        int testLevel,
        int correct,
        int total,
        Map<Integer, Integer> correctByLevel,
        List<WrongAnswer> wrongAnswers,
        Map<String, Integer> topicsToRepeat,
        TestCompleted event
) {
    public record WrongAnswer(
            String question,
            String chosenAnswer,
            String correctAnswer,
            String explanation
    ) {
    }
}
