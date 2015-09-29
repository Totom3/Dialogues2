package io.github.totom3.dialogues;

import org.bukkit.entity.Player;

/**
 *
 * @author Totom3
 */
public interface MessageFormatter {

    String formatPromptMessage(DialogueSession session, DialoguePrompt prompt);

    String formatChatMessage(DialogueSession session, Player sender, InputChoice choice);

    String formatDisplayMessage(DialogueSession session, InputChoice choice);

}
