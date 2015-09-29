package io.github.totom3.dialogues;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Totom3
 */
public class DialoguesCommandExecutor implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
	if (args.length == 0) {
	    return false;
	}

	String subCommand = args[0].toLowerCase();
	switch (subCommand) {
	    case "compile":
		compile(sender, args);
		break;
	    case "load":
		load(sender, args);
		break;
	    case "unload":
		unload(sender, args);
		break;
	    case "list":
		list(sender, args);
		break;
	    case "start":
		start(sender, args);
		break;
	    case "help":
		help(sender, args);
		break;
	    case "select":
		select(sender, args);
		break;
	    default:
		return false;
	}

	return true;
    }

    private void compile(CommandSender sender, String[] args) {
	if (args.length != 2) {
	    sender.sendMessage(ChatColor.DARK_RED + "Syntax: " + ChatColor.RED + "/dialogues compile <dialogue>");
	    return;
	}

	DialoguesCache cache = DialoguesCache.get();

	String name = args[1];
	Dialogue dialogue;
	try {
	    dialogue = cache.loadFromExcel(name);
	} catch (ExecutionException ex) {
	    Throwable cause = ex.getCause();
	    if (cause instanceof FileNotFoundException) {
		sender.sendMessage(ChatColor.DARK_RED + "Error: " + ChatColor.RED + cause.getMessage());
		return;
	    }
	    sender.sendMessage(ChatColor.DARK_RED + "Error while loading dialogue: " + ChatColor.RED + cause);
	    Main.get().getLogger().log(Level.SEVERE, "Could not load dialogue '" + name + "'", cause);
	    return;
	}

	try {
	    cache.saveToBinary(dialogue);
	    sender.sendMessage(ChatColor.GREEN + "Successfullly compiled dialogue " + ChatColor.DARK_GREEN + name + ChatColor.GREEN + "!");
	} catch (IOException ex) {
	    sender.sendMessage(ChatColor.DARK_RED + "Error while saving dialogue: " + ChatColor.RED + ex);
	    Main.get().getLogger().log(Level.SEVERE, "Could not save dialogue '" + name + "'", ex);
	}
    }

    private void load(CommandSender sender, String[] args) {
	if (args.length != 2) {
	    sender.sendMessage(ChatColor.DARK_RED + "Syntax: " + ChatColor.RED + "/dialogues load <dialogue>");
	    return;
	}

	String name = args[1];
	DialoguesCache cache = DialoguesCache.get();

	if (cache.isLoaded(name)) {
	    sender.sendMessage(ChatColor.YELLOW + "Dialogue " + ChatColor.GOLD + name + ChatColor.YELLOW + " is already loaded.");
	    return;
	}

	if (loadDialogue(name, sender) != null) {
	    sender.sendMessage(ChatColor.GREEN + "Successfully loaded dialogue " + ChatColor.DARK_GREEN + name + ChatColor.GREEN + "!");
	}

    }

    private void unload(CommandSender sender, String[] args) {
	if (args.length != 2) {
	    sender.sendMessage(ChatColor.DARK_RED + "Syntax: " + ChatColor.RED + "/dialogues unload <dialogue OR -ALL>");
	    return;
	}

	DialoguesCache cache = DialoguesCache.get();
	String name = args[1];

	if (name.equalsIgnoreCase("-ALL")) {
	    int size = cache.size();
	    cache.clear();
	    sender.sendMessage(ChatColor.GREEN + "Successfully unloaded " + ChatColor.DARK_GREEN + size + ChatColor.GREEN + " dialogues!");
	    return;
	}

	if (cache.unload(name) == null) {
	    sender.sendMessage(ChatColor.YELLOW + "The dialogue " + ChatColor.GOLD + name + ChatColor.YELLOW + " was not loaded");
	} else {
	    sender.sendMessage(ChatColor.GREEN + "Successfully unloaded dialogue " + ChatColor.DARK_GREEN + name + ChatColor.GREEN + "!");
	}
    }

    private void list(CommandSender sender, String[] args) {
	Set<String> dialogues = DialoguesCache.get().getDialogues().keySet();

	StringBuilder builder = new StringBuilder();
	builder.append(ChatColor.GOLD).append("Listing all loaded dialogues: ").append(ChatColor.YELLOW);
	builder.append(StringUtils.join(dialogues, ", "));

	sender.sendMessage(builder.toString());
    }

    private void start(CommandSender sender, String[] args) {
	if (args.length < 2) {
	    sender.sendMessage(ChatColor.DARK_RED + "Syntax: " + ChatColor.RED + "/dialogues start <dialogue> [participant1, participant2, ...]");
	    return;
	}

	String dialogueName = args[1];
	Dialogue dialogue = loadDialogue(dialogueName, sender);
	if (dialogue == null) {
	    return;
	}

	Set<Player> participants = new HashSet<>();
	if (sender instanceof Player) {
	    participants.add((Player) sender);
	}

	for (int i = 2; i < args.length; ++i) {
	    String playerName = args[i];
	    Player player = Bukkit.getPlayer(playerName);
	    if (player == null) {
		sender.sendMessage(ChatColor.DARK_RED + "Error: " + ChatColor.RED + "could not find player '" + playerName + "'");
		return;
	    }

	    if (!participants.add(player)) {
		sender.sendMessage(ChatColor.DARK_RED + "Error: " + ChatColor.RED + "player '" + player.getName() + "' entered twice");
		return;
	    }
	}

	if (participants.isEmpty()) {
	    sender.sendMessage(ChatColor.DARK_RED + "Error: " + ChatColor.RED + "you must be a player or must specify other participants in order to use this command.");
	}

	DialogueSession session = dialogue.makeSession(participants);
	session.start();
    }

    private void help(CommandSender sender, String[] args) {
	sender.sendMessage(ChatColor.GOLD + "----=[ " + ChatColor.YELLOW + "Displaying help for /dialogues (/dlgs)" + ChatColor.GOLD + " ]=----");
	sender.sendMessage(ChatColor.YELLOW + "- /dlgs... ");
	sender.sendMessage(ChatColor.YELLOW + "   -> compile <dialogue>");
	sender.sendMessage(ChatColor.YELLOW + "   -> load    <dialogue>");
	sender.sendMessage(ChatColor.YELLOW + "   -> unload  <dialogue OR -ALL>");
	sender.sendMessage(ChatColor.YELLOW + "   -> start   <dialogue> [participant1, participant2, ...]");
	sender.sendMessage(ChatColor.YELLOW + "   -> list ");
	sender.sendMessage(ChatColor.YELLOW + "   -> help ");
    }

    private void select(CommandSender sender, String[] args) {
	if (args.length != 2) {
	    return;
	}

	if (!(sender instanceof Player)) {
	    return;
	}

	Player player = (Player) sender;
	DialogueSession session = DialogueSessionsManager.get().getSessionOf(player);
	if (session == null) {
	    return;
	}

	int choice;
	try {
	    choice = Integer.parseUnsignedInt(args[1]);
	} catch (NumberFormatException ex) {
	    return;
	}

	if (choice == 0) {
	    return;
	}

	session.acceptChoice(player, choice);
    }

    // ------------------=[ Utility Methods ]=------------------
    private Dialogue loadDialogue(String name, CommandSender sender) {
	try {
	    return DialoguesCache.get().getOrLoad(name);
	} catch (ExecutionException ex) {
	    Throwable cause = ex.getCause();
	    if (cause instanceof FileNotFoundException) {
		sender.sendMessage(ChatColor.DARK_RED + "Error: " + ChatColor.RED + cause.getMessage());
		return null;
	    }
	    sender.sendMessage(ChatColor.DARK_RED + "Error: " + ChatColor.RED + cause);
	    Main.get().getLogger().log(Level.SEVERE, "Could not load dialogue '" + name + "'", cause);
	    return null;
	}
    }
}
