package io.github.totom3.dialogues;

import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 *
 * @author Totom3
 */
public class DefaultMessageFormatter implements MessageFormatter {

    @Override
    public String formatPromptMessage(DialogueSession session, DialoguePrompt prompt) {
	String message = prompt.message();
	if (message == null) {
	    return null;
	}

	message = applyPrefix(session, message);
	return applyVariablesAndColors(session, message).toString();
    }

    @Override
    public String formatChatMessage(DialogueSession session, Player sender, InputChoice choice) {
	String message = choice.chatMessage();
	if (message == null) {
	    return null;
	}

	message = applyPrefix(session, message);
	return applyVariablesAndColors(session, message).toString();
    }

    @Override
    public String formatDisplayMessage(DialogueSession session, InputChoice choice) {
	String message = choice.displayMessage();
	if (message == null) {
	    return null;
	}

	StringBuilder builder = new StringBuilder();
	builder.append(ChatColor.GOLD).append(" ").append(choice.choiceID()).append(". ").append(ChatColor.YELLOW);

	StringBuffer messageBuffer = applyVariablesAndColors(session, message);
	builder.append(messageBuffer);

	return builder.toString();
    }

    protected String applyPrefix(DialogueSession session, String message) {
	char firstChar = message.charAt(0);
	String prefix = session.getDialogue().getMessagePrefix(firstChar);
	if (prefix == null) {
	    return message;
	}

	return prefix + message.substring(1);
    }

    protected StringBuffer applyVariablesAndColors(DialogueSession session, String message) {
	String playersString = null; // only evaluated if needed

	Pattern pattern = Pattern.compile("(%(s|a|r))|([&" + ChatColor.COLOR_CHAR + "]([0-9a-fk-or]))");
	Matcher matcher = pattern.matcher(message);

	// externally synchronizing on string buffer for performance reasons
	StringBuffer sb = new StringBuffer(message.length());
	synchronized (sb) {
	    ChatColor lastColor = null;
	    ChatColor lastFormat = null;
	    while (matcher.find()) {
		String group = matcher.group(2);
		if (group != null) {    // Found variable
		    if (group.equals("r")) {
			matcher.appendReplacement(sb, session.getRandomPlayer().getDisplayName());
			if (lastColor != null) {
			    sb.append(lastColor);
			}
			if (lastFormat != null) {
			    sb.append(lastFormat);
			}
		    } else if (group.equals("a")) {
			if (playersString == null) {
			    playersString = formatPlayers(session.getParticipants(), lastColor, lastFormat);
			}
			matcher.appendReplacement(sb, playersString);
		    } else if (group.equals("s")) {
			Player lastSender = session.getLastSender();
			matcher.appendReplacement(sb, (lastSender != null) ? lastSender.getDisplayName() : "null");
			if (lastColor != null) {
			    sb.append(lastColor);
			}
			if (lastFormat != null) {
			    sb.append(lastFormat);
			}
		    }
		} else { // Found color
		    group = matcher.group(4);
		    char c = group.charAt(0);
		    ChatColor cc = ChatColor.getByChar(c);
		    if (cc.isColor() || cc == ChatColor.RESET) {
			lastColor = cc;
			lastFormat = null;
		    } else {
			lastFormat = cc;
		    }
		    matcher.appendReplacement(sb, String.valueOf(new char[]{ChatColor.COLOR_CHAR, c}));
		}

	    }

	    matcher.appendTail(sb);
	    return sb;
	}
    }

    protected String formatPlayers(Collection<Player> players, ChatColor color, ChatColor format) {
	Iterator<Player> it = players.iterator();

	if (!it.hasNext()) {
	    return "";
	}

	StringBuilder b = new StringBuilder(players.size() * 7); // estimation of players' display names length

	int iSize = players.size() - 2;
	int i = 0;
	for (;;) {
	    b.append(it.next().getDisplayName());

	    if (color != null) {
		b.append(color);
	    }
	    if (format != null) {
		b.append(format);
	    }

	    if (!it.hasNext()) {
		return b.toString();
	    }

	    if (i == iSize) {
		b.append(", and ");
	    } else {
		b.append(", ");
	    }

	    ++i;
	}
    }
}
