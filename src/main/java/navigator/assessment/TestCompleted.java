package navigator.assessment;

public record TestCompleted(
        long userId,
        long skillId,
        int testLevel,
        int correct,
        int total
) {
}
