package io.github.totom3.dialogues;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Totom3
 */
public class DialoguePrompt {

    private Dialogue dialogue;

    private final String message;
    private final int nextPromptID;
    private final int promptID;
    private final int finalDelay;
    private final int initialDelay;
    private final int timeout;
    private final List<InputChoice> choices;

    DialoguePrompt(String message, int promptID, int nextPromptID, int initialDelay, int finalDelay) {
	this(message, promptID, nextPromptID, initialDelay, finalDelay, 0, null);
    }

    DialoguePrompt(String message, int promptID, int nextPromptID,
	    int initialDelay, int finalDelay, int timeout, List<InputChoice> choices) {
	this.message = ((StringUtils.isBlank(message)) ? null : message);
	this.nextPromptID = Math.max(-1, nextPromptID);
	this.promptID = promptID;
	this.initialDelay = Math.max(0, initialDelay);
	this.finalDelay = Math.max(0, finalDelay);
	this.timeout = Math.max(0, timeout);
	this.choices = ((choices == null) ? ImmutableList.of() : ImmutableList.copyOf(choices));
    }

    void init(Dialogue dialogue) {
	if (this.dialogue != null) {
	    throw new IllegalStateException("already initialized");
	}
	this.dialogue = checkNotNull(dialogue);
    }

    public String message() {
	return message;
    }

    public boolean hasMessage() {
	return message != null;
    }

    public Dialogue dialogue() {
	return dialogue;
    }

    public int nextPromptID() {
	return nextPromptID;
    }

    public DialoguePrompt nextPrompt() {
	return dialogue.getPrompt(nextPromptID);
    }

    public int promptID() {
	return promptID;
    }

    public int initialDelay() {
	return initialDelay;
    }

    public int finalDelay() {
	return finalDelay;
    }

    public boolean hasInitialDelay() {
	return initialDelay > 0;
    }

    public boolean hasFinalDelay() {
	return finalDelay > 0;
    }

    public boolean requiresChoices() {
	return !choices.isEmpty();
    }

    public int choiceTimeout() {
	return timeout;
    }

    public boolean hasChoiceTimeout() {
	return timeout > 0;
    }

    public List<InputChoice> inputChoices() {
	return choices;
    }

    public InputChoice inputChoice(int choice) {
	if (choice <= 0) {
	    return null;
	}
	
	if (choices.size() < choice) {
	    return null;
	}
	
	return choices.get(choice - 1);
    }
}
