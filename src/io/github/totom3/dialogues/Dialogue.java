package io.github.totom3.dialogues;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import org.bukkit.entity.Player;

/**
 *
 * @author Totom3
 */
public class Dialogue {

    private final String name;
    private final int firstPrompt;
    private final Map<Integer, DialoguePrompt> prompts;
    private final Map<Character, String> prefixes;

    Dialogue(String name, int firstPrompt, Map<Integer, DialoguePrompt> prompts, Map<Character, String> prefixes) {
	this.name = checkNotNull(name);
	this.firstPrompt = firstPrompt;
	this.prefixes = ImmutableMap.copyOf(prefixes);
	this.prompts = ImmutableMap.copyOf(prompts);

	if (prompts.get(firstPrompt) == null) {
	    throw new IllegalArgumentException("First prompt " + firstPrompt + " does not exist in prompts: " + prompts);
	}
    }

    public String getName() {
	return name;
    }

    public int firstPromptID() {
	return firstPrompt;
    }

    public DialoguePrompt firstPrompt() {
	return prompts.get(firstPrompt);
    }

    public DialoguePrompt getPrompt(int promptID) {
	return prompts.get(promptID);
    }

    public DialogueSession makeSession(Collection<Player> players) {
	return new DialogueSession(this, players);
    }

    public Map<Integer, DialoguePrompt> prompts() {
	// prompts field will always be an ImmutableMap; it's safe to return the instance itself
	return prompts;
    }

    public Map<Character, String> messagePrefixes() {
	return prefixes;
    }

    public String getMessagePrefix(char character) {
	return prefixes.get(character);
    }
}
