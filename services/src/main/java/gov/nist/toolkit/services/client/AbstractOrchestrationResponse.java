package gov.nist.toolkit.services.client;

import gov.nist.toolkit.results.client.Test;
import gov.nist.toolkit.results.client.TestInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for Toolkit service responses for orchestration building.
 * Encapsulates MessageItems, tests which are run to initialize a SUT or sim
 * for conformance testing.
 */
abstract public class AbstractOrchestrationResponse extends RawResponse {
//    private List<TestInstance> orchestrationTests = new ArrayList<>();  // test definitions used to build the orchestration
    private List<MessageItem> messages = new ArrayList<>();
    private String additionalDocumentation = null;
    private boolean wasStarted = true;  // many services do not load this - check first
    private Map<TestInstance, Map<String,String>> testParams = new HashMap<>();

    public AbstractOrchestrationResponse() {}
    /**
     * Does vendor initiate first message of test?
     * @return true if they do.
     */
    abstract public boolean isExternalStart();

    public MessageItem addMessage(TestInstance testInstance, boolean success, String message) {
        MessageItem item = new MessageItem(testInstance, success, message);
        messages.add(item);
        if (!success) {
            this.errorMessage = message;
            this.error = true;
        }
        return item;
    }

    public Collection<MessageItem> getMessages() { return messages; }

    public boolean hasError() {
        for (MessageItem item : messages) {
            if (!item.isSuccess())
                return true;
        }
        return false;
    }

    /**
     * Tests used to build up test environment
     * @return
     */
    public List<TestInstance> getTestInstances() {
        List<TestInstance> testInstances = new ArrayList<>();
        for (MessageItem item : messages) {
            testInstances.add(item.getTestInstance());
        }
        return testInstances;
    }

    public MessageItem getItemForTest(TestInstance testInstance) {
        for (MessageItem item : messages) {
            if (item.getTestInstance().equals(testInstance))
                return item;
        }
        return null;
    }

    public String getAdditionalDocumentation() {
        return additionalDocumentation;
    }

    public void setAdditionalDocumentation(String additionalDocumentation) {
        this.additionalDocumentation = additionalDocumentation;
    }

    public boolean hasAdditionalDocumentation() {
        return additionalDocumentation != null;
    }

    public boolean isWasStarted() {
        return wasStarted;
    }

    public void setWasStarted(boolean wasStarted) {
        this.wasStarted = wasStarted;
    }

    public Map<TestInstance, Map<String, String>> getTestParams() {
        return testParams;
    }

    public void setTestParams(Map<TestInstance, Map<String, String>> testParams) {
        this.testParams = testParams;
    }
}
