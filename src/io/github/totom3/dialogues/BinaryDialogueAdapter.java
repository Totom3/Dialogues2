package io.github.totom3.dialogues;

import io.github.totom3.commons.binary.BinaryAdapter;
import io.github.totom3.commons.binary.DeserializationContext;
import io.github.totom3.commons.binary.DeserializingException;
import io.github.totom3.commons.binary.SerializationContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Totom3
 */
public class BinaryDialogueAdapter implements BinaryAdapter<Dialogue> {

    private static final String CORRUPTED_MSG = " (file is likely corrupted, try re-compiling)";

    @Override
    public Dialogue read(DeserializationContext context) throws IOException {
	String name = context.getData("name", String.class);

	// Read first prompt ID
	int firstPrompt = context.readInt();
	if (firstPrompt < 0) {
	    throw new DeserializingException("read invalid first prompt " + firstPrompt + CORRUPTED_MSG);
	}

	// Read prefixes
	Map<Character, String> prefixes = context.readMap(Character.class, String.class);

	// Read prompts
	int size = context.readInt();
	Map<Integer, DialoguePrompt> prompts = new HashMap<>(size);
	for (int i = 0; i < size; ++i) {
	    // Read prompt ID
	    int id = context.readInt();

	    prompts.put(id, readPrompt(id, context));
	}
	
	
	// Last step: creating Dialogue and linking prompts to it
	Dialogue dialogue = new Dialogue(name, firstPrompt, prompts, prefixes);
	for (DialoguePrompt prompt : prompts.values()) {
	    prompt.init(dialogue);
	}
	
	return dialogue;
    }

    private DialoguePrompt readPrompt(int promptID, DeserializationContext context) throws IOException {
	// Read message
	String message = context.readString();

	// Read delays
	int initialDelay = context.readInt();
	int finalDelay = context.readInt();

	// Read next promptID
	int nextPromptID = context.readInt();

	// Read timeout
	int timeout = context.readInt();

	// Read choices
	int size = context.readInt();
	List<InputChoice> choices = new ArrayList<>(size);
	for (int i = 0; i < size; ++i) {
	    // Retrieve choice ID
	    int choiceID = i + 1;

	    // Read next prompt ID
	    int choiceNextPromptID = context.readInt();

	    // Read display message
	    String dispMessage = context.readString();

	    // Read chat message
	    String chatMessage = context.readString();
	    
	    choices.add(new InputChoice(choiceID, choiceNextPromptID, dispMessage, chatMessage));
	}
	
	return new DialoguePrompt(message, promptID, nextPromptID, initialDelay, finalDelay, timeout, choices);
    }

    // --------------------------------=[ Write Methods ]=--------------------------------
    @Override
    public void write(Dialogue dialogue, SerializationContext context) throws IOException {
	// Write first prompt ID
	context.writeInt(dialogue.firstPromptID());

	// Write prefixes
	context.writeMap(dialogue.messagePrefixes());

	// Write prompts
	Map<Integer, DialoguePrompt> prompts = dialogue.prompts();

	context.writeInt(prompts.size());
	for (Entry<Integer, DialoguePrompt> entry : prompts.entrySet()) {
	    int id = entry.getKey();
	    DialoguePrompt prompt = entry.getValue();

	    // Write ID
	    context.writeInt(id);

	    // Write prompt
	    writePrompt(prompt, context);
	}
    }

    private void writePrompt(DialoguePrompt prompt, SerializationContext context) throws IOException {
	// Write message
	context.writeString(prompt.message());

	// Write delays
	context.writeInt(prompt.initialDelay());
	context.writeInt(prompt.finalDelay());

	// Write next prompt ID
	context.writeInt(prompt.nextPromptID());

	// Write timeout
	context.writeInt(prompt.choiceTimeout());

	// Write choices
	List<InputChoice> choices = prompt.inputChoices();
	context.writeInt(choices.size());
	for (InputChoice choice : choices) {
	    // Write next prompt ID
	    context.writeInt(choice.nextPromptID());

	    // Write display message
	    context.writeString(choice.displayMessage());

	    // Write chat message
	    context.writeString(choice.chatMessage());
	}
    }

}
