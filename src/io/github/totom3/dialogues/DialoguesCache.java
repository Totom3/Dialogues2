package io.github.totom3.dialogues;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import io.github.totom3.commons.binary.SerializingException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import jxl.read.biff.BiffException;

/**
 *
 * @author Totom3
 */
public class DialoguesCache {

    public static final File BASE_FILE = Main.get().getDataFolder();

    private static final DialoguesCache instance = new DialoguesCache();

    static {
	if (!BASE_FILE.isDirectory()) {
	    BASE_FILE.mkdirs();
	}
    }

    public static DialoguesCache get() {
	return instance;
    }

    public static File getExcelFile(String name) {
	name = name.replace('.', File.separatorChar);
	name += ".xls";
	return new File(BASE_FILE, name);
    }

    public static File getBinaryFile(String name) {
	name = name.replace('.', File.separatorChar);
	name += ".dlg";
	return new File(BASE_FILE, name);
    }

    private final LoadingCache<String, Dialogue> dialogues;
    private final ExcelDialogueLoader excelLoader = new ExcelDialogueLoader();
    private final BinaryDialogueLoader binaryLoader = new BinaryDialogueLoader();

    public DialoguesCache() {
	dialogues = CacheBuilder.<String, Dialogue>newBuilder()
		.softValues()
		.build(binaryLoader);
    }

    public Map<String, Dialogue> getDialogues() {
	return Collections.unmodifiableMap(dialogues.asMap());
    }

    public Dialogue getIfPresent(String name) {
	return dialogues.getIfPresent(check(name));
    }

    public Dialogue getOrLoad(String name) throws ExecutionException {
	return dialogues.get(check(name));
    }

    public Dialogue unload(String name) {
	Dialogue dialogue = dialogues.getIfPresent(check(name));
	dialogues.invalidate(name);
	return dialogue;
    }

    public Dialogue tryGetOrLoad(String name) {
	try {
	    return getOrLoad(check(name));
	} catch (ExecutionException ex) {
	    Main.get().getLogger().log(Level.SEVERE, "Could not load dialogue '" + name + "'", ex);
	    return null;
	}
    }

    public Dialogue loadFromExcel(String name) throws ExecutionException {
	Dialogue dialogue;
	try {
	    dialogue = excelLoader.load(name);
	} catch (IOException | BiffException ex) {
	    throw new ExecutionException(ex);
	}

	dialogues.put(name, dialogue);
	return dialogue;
    }

    public void saveToBinary(Dialogue dialogue) throws IOException {
	if (dialogue == null) {
	    throw new NullPointerException("Cannot save null dialogue");
	}

	try {
	    binaryLoader.save(dialogue);
	} catch (IOException ex) {
	    throw new SerializingException("Could not save dialogue '" + dialogue.getName() + "'", ex);
	}
    }

    public boolean isLoaded(String name) {
	return dialogues.getIfPresent(check(name)) != null;
    }

    public void clear() {
	dialogues.invalidateAll();
    }

    public int size() {
	return (int) dialogues.size();
    }

    public boolean isEmpty() {
	return dialogues.size() == 0;
    }

    private String check(String name) {
	if (name == null) {
	    throw new IllegalArgumentException("Dialogue name cannot be null");
	}
	return name;
    }

}
