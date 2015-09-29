package io.github.totom3.dialogues;

import com.google.common.cache.CacheLoader;
import io.github.totom3.commons.binary.DeserializationContext;
import io.github.totom3.commons.binary.SerializationContext;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * @author Totom3
 */
public class BinaryDialogueLoader extends CacheLoader<String, Dialogue> {

    @Override
    public Dialogue load(String name) throws Exception {
	if (name == null) {
	    throw new NullPointerException("Dialogue name cannot be null");
	}

	File file = DialoguesCache.getBinaryFile(name);
	if (!file.isFile()) {
	    throw new FileNotFoundException("Missing binary file for dialogue '" + name + "'");
	}

	/**
	 * File & Directory names on Windows are case-unsensitive. This can
	 * represent a problem when combined with a case-sentive cache (such as
	 * the Dialogues cache). The same dialogue could be loaded more than
	 * once, with different names: "test", "TEST", "TesT" and "TESt" would
	 * all refer to the same file, but would be considered as different
	 * dialogues by the DialoguesCache. The measure belows makes sure that
	 * doesn't happen by enforcing case-sensitivity.
	 */
	String canonicalPath = file.getCanonicalPath();
	if (!canonicalPath.endsWith(name.replace('.', File.separatorChar).concat(".dlg"))) {
	    throw new FileNotFoundException("Missing binary file for dialogue '" + name + "'");
	}

	try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
	    DeserializationContext context = new DeserializationContext(in);
	    context.setData("name", name);
	    return context.readObject(Dialogue.class);
	}
    }

    public void save(Dialogue dialogue) throws IOException {
	String name = dialogue.getName();
	File file = DialoguesCache.getBinaryFile(name);
	if (!file.isFile()) {
	    file.getParentFile().mkdirs();
	    file.createNewFile();
	}

	try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
	    SerializationContext context = new SerializationContext(out);
	    context.writeObject(dialogue);
	}
    }

}
