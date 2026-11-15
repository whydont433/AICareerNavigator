package navigator.assessment;

import java.util.List;

public record TestQuestion(
        long id,
        long skillId,
        int level,
        String question,
        List<String> options,
        int correctIndex,
        String explanation,
        String topic
) {
    public TestQuestion {
        options = List.copyOf(options);
    }
}
