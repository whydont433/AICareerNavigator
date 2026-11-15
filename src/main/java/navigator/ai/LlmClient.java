package navigator.ai;

public interface LlmClient {
    String completeJson(String promptFileName, String userPayload);
}
