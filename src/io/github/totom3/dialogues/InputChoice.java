package io.github.totom3.dialogues;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Totom3
 */
public class InputChoice {

    private final int choiceID;
    private final int nextPromptID;
    private final String dispMessage;
    private final String chatMessage;

    public InputChoice(int choiceID, int nextPromptID, String dispMessage, String chatMessage) {
	this.choiceID = choiceID;
	this.nextPromptID = nextPromptID;
	this.dispMessage = (StringUtils.isBlank(dispMessage) ? null : dispMessage);
	this.chatMessage = (StringUtils.isBlank(chatMessage) ? null : chatMessage);
    }

    public int choiceID() {
	return choiceID;
    }

    public int nextPromptID() {
	return nextPromptID;
    }

    public String displayMessage() {
	return dispMessage;
    }

    public String chatMessage() {
	return chatMessage;
    }
}
