package io.github.totom3.dialogues;

import io.github.totom3.commons.binary.BinaryIO;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Totom3
 */
public class Main extends JavaPlugin {

    private static Main instance;

    public static Main get() {
	if (instance == null) {
	    throw new IllegalStateException("plugin not initialized");
	}
	return instance;
    }

    public Main() {
	instance = this;
	BinaryIO.get().registerAdapter(Dialogue.class, new BinaryDialogueAdapter());
    }

    @Override
    public void onEnable() {
	DialogueSessionsManager.get().init();
	getCommand("dialogues").setExecutor(new DialoguesCommandExecutor());
    }
    
    
}
