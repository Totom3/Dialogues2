package io.github.totom3.dialogues;

import static com.google.common.base.Preconditions.checkNotNull;
import io.github.totom3.commons.chat.ChatClickAction;
import io.github.totom3.commons.chat.ChatClickEvent;
import io.github.totom3.commons.chat.ChatComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 *
 * @author Totom3
 */
public class DialogueSession implements Runnable {

    private static final Random random = new Random();

    /**
     * Before initial delay. Executes the latter.
     */
    private static final int ACCEPT_PROMPT = 0x1;

    /**
     * After initial delay. Sends message and executes final delay.
     */
    private static final int SEND_MESSAGE = 0x2;

    /**
     * After final delay. Moves on to next prompt or displays choices.
     */
    private static final int POST_MESSAGE = 0x4;

    /**
     * The dialogue of this session.
     */
    private final Dialogue dialogue;

    /**
     * The list of participants. Using a List over a Set because it is more
     * likely to be used for message broadcasting iteration, and for random
     * access.
     */
    private final List<Player> participants;

    /**
     * The current prompt, or null if not started or ended.
     */
    private DialoguePrompt currentPrompt;

    /**
     * The message formatter for this dialogue session. May be changed
     * throughout execution.
     */
    private MessageFormatter formatter;

    private int action;
    private boolean started;
    private boolean acceptsInput;
    private boolean timeoutScheduled;

    private BukkitTask waitingTask;
    private Player lastSender;

    public DialogueSession(Dialogue dialogue, Collection<Player> participants) {
	this(dialogue, participants, new DefaultMessageFormatter());
    }

    public DialogueSession(Dialogue dialogue, Collection<Player> participants, MessageFormatter formatter) {
	this.dialogue = checkNotNull(dialogue);
	this.participants = new ArrayList<>(participants);
	this.formatter = checkNotNull(formatter);

	if (participants.isEmpty()) {
	    throw new IllegalArgumentException("Cannot create dialogue session with no participants");
	}
    }

    // TODO: remove method
    private void jumpToPrompt(DialoguePrompt prompt) {
	currentPrompt = prompt;
	System.out.println("Jumped to prompt " + ((prompt == null) ? "null" : prompt.promptID()));
    }

    public void start() {
	if (started) {
	    throw new IllegalStateException("Session is already started");
	}

	jumpToPrompt(dialogue.firstPrompt());
	if (currentPrompt == null) {
	    throw new IllegalStateException("Dialogue doesn't have a first prompt.");
	}

	DialogueSessionsManager.get().onStart(this);
	started = true;
	action = ACCEPT_PROMPT;
	run();
    }

    @Override
    public void run() {
	if (currentPrompt == null) {
	    terminate();
	    return;
	}

	if (timeoutScheduled) {
	    timeoutScheduled = false;
	    handlePostTimeout();
	    return;
	}

	switch (action) {
	    case ACCEPT_PROMPT:
		handleAcceptPrompt();
		break;
	    case SEND_MESSAGE:
		handleSendMessage();
		break;
	    case POST_MESSAGE:
		handlePostMessage();
		break;
	    default:
		throw new AssertionError("unexpected action " + action);
	}
    }

    public Collection<Player> getParticipants() {
	return Collections.unmodifiableList(participants);
    }

    public Dialogue getDialogue() {
	return dialogue;
    }

    public MessageFormatter getFormatter() {
	return formatter;
    }

    public void setFormatter(MessageFormatter format) {
	this.formatter = checkNotNull(format);
    }

    public Player getLastSender() {
	if (!lastSender.isOnline()) {
	    lastSender = null;
	}
	return lastSender;
    }

    public Player getRandomPlayer() {
	return participants.get(random.nextInt(participants.size()));
    }

    void acceptChoice(Player player, int choiceID) {
	if (!acceptsInput) {
	    return;
	}

	if (currentPrompt == null) {
	    return;
	}

	InputChoice inputChoice = currentPrompt.inputChoice(choiceID);
	if (inputChoice == null) {
	    return;
	}

	// Input accepted!
	lastSender = player;

	acceptsInput = false;
	if (timeoutScheduled) {
	    timeoutScheduled = false;
	    cancelTask();
	}

	String msg = formatter.formatChatMessage(this, player, inputChoice);
	if (msg != null) {
	    broadcastMessage(msg);
	}

	jumpToPrompt(dialogue.getPrompt(inputChoice.nextPromptID()));
	action = ACCEPT_PROMPT;
	run();
    }

    private void handleAcceptPrompt() {
	// immediately send message if no initial delay
	if (!currentPrompt.hasInitialDelay()) {
	    action = SEND_MESSAGE;
	    run();
	    return;
	}
	action = SEND_MESSAGE;
	rescheduleIn(currentPrompt.initialDelay());
    }

    private void handleSendMessage() {
	if (currentPrompt.hasMessage()) {
	    String msg = formatter.formatPromptMessage(this, currentPrompt);
	    broadcastMessage(msg);
	}

	if (!currentPrompt.hasFinalDelay()) {
	    action = POST_MESSAGE;
	    run();
	    return;
	}

	action = POST_MESSAGE;
	rescheduleIn(currentPrompt.finalDelay());
    }

    private void handlePostMessage() {
	if (!currentPrompt.requiresChoices()) {
	    jumpToPrompt(currentPrompt.nextPrompt());
	    action = ACCEPT_PROMPT;
	    run();
	    return;
	}

	List<InputChoice> choices = currentPrompt.inputChoices();
	for (InputChoice choice : choices) {
	    String msg = formatter.formatDisplayMessage(this, choice);
	    if (msg != null) {
		broadcastCommandMessage(msg, "/dlgs select " + choice.choiceID());
	    }
	}

	if (currentPrompt.hasChoiceTimeout()) {
	    rescheduleIn(currentPrompt.choiceTimeout());
	    timeoutScheduled = true;
	}

	acceptsInput = true;
    }

    private void handlePostTimeout() {
	cancelTask();
	acceptsInput = false;

	jumpToPrompt(currentPrompt.nextPrompt());
	action = ACCEPT_PROMPT;
	run();
    }

    private void rescheduleIn(int delay) {
	cancelTask();
	waitingTask = Bukkit.getScheduler().runTaskLater(Main.get(), this, delay);
    }

    private void cancelTask() {
	if (waitingTask != null) {
	    waitingTask.cancel();
	    waitingTask = null;
	}
    }

    private void broadcastMessage(String msg) {
	for (Player player : participants) {
	    player.sendMessage(msg);
	}
    }

    private void broadcastCommandMessage(String msg, String command) {
	ChatComponent comp = ChatComponent.fromPlainText(msg);
	comp.setClickEvent(new ChatClickEvent(ChatClickAction.RUN_COMMAND, command));
	for (Player player : participants) {
	    comp.send(player);
	}
    }

    public void terminate() {
	terminate(true);
    }

    void terminate(boolean notifyManager) {
	if (notifyManager) {
	    DialogueSessionsManager.get().onStop(this);
	}
	
	cancelTask();
	acceptsInput = false;
	timeoutScheduled = false;
	currentPrompt = null;
	started = false;
	participants.clear();
	System.out.println("Session terminated!");
	
    }

    /**
     * Removes internally a participant and return whether or not there are no
     * more participants.
     * @param player the player to remove.
     * @return {@code true} if there are still one or more participant(s),
     *         {@code false} otherwise.
     */
    boolean removeParticipant(Player player) {
	participants.remove(player);
	return !participants.isEmpty();
    }
}
