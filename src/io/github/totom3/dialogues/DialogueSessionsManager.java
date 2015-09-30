package io.github.totom3.dialogues;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 *
 * @author Totom3
 */
public class DialogueSessionsManager implements Listener {

    private static final DialogueSessionsManager instance = new DialogueSessionsManager();

    public static DialogueSessionsManager get() {
	return instance;
    }

    private final Map<Player, DialogueSession> sessions = new WeakHashMap<>();

    void init() {
	Bukkit.getPluginManager().registerEvents(this, Main.get());
    }

    public Map<Player, DialogueSession> getSessions() {
	return sessions;
    }

    public DialogueSession getSessionOf(Player player) {
	return sessions.get(player);
    }

    public boolean hasSession(Player player) {
	return sessions.get(player) != null;
    }

    void onStart(DialogueSession session) {
	Collection<Player> participants = new HashSet<>(session.getParticipants());
	for (Player player : participants) {
	    DialogueSession oldSession = sessions.put(player, session);
	    if (oldSession != null && oldSession != session && !oldSession.removeParticipant(player)) {
		oldSession.terminate(false);
	    }
	}
    }

    void onStop(DialogueSession session) {
	System.out.println("Receiving stop signal. Removing participants.");
	Collection<Player> participants = session.getParticipants();
	for (Player player : participants) {
	    sessions.remove(player);
	}
	System.out.println("Sessions=" + sessions);
    }

    @EventHandler
    private void on(PlayerQuitEvent event) {
	onQuit(event.getPlayer());
    }

    @EventHandler
    private void on(PlayerKickEvent event) {
	onQuit(event.getPlayer());
    }

    private void onQuit(Player player) {
	DialogueSession session = sessions.remove(player);
	if (session != null && !session.removeParticipant(player)) {
	    session.terminate(false);
	}
    }
}
