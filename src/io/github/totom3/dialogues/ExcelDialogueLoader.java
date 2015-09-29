package io.github.totom3.dialogues;

import com.google.common.base.Splitter;
import io.github.totom3.commons.binary.DeserializingException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Totom3
 */
public class ExcelDialogueLoader {

    private static final String START_VAR = "start";
    private static final String PREFIX_VAR = "prefixes";
    private static final String PROMPT_IDENTIFIER = "prompt";

    // <editor-fold defaultstate="collapsed" desc=" Utility Methods ">
    private static String getContents(Cell[] row, int index) {
	return (index >= row.length) ? "" : row[index].getContents();
    }

    private static String malformedPrefix(String contents, int row, int column) {
	return "Malformed prefix statement '" + contents + "' (at row " + row + " and column " + column + ")";
    }

    private static int parsePositive(String contents, int currentRow, String type) throws DeserializingException {
	if (StringUtils.isBlank(contents)) {
	    return 0;
	}

	try {
	    return Integer.parseUnsignedInt(contents);
	} catch (NumberFormatException ex) {
	    throw new DeserializingException("Could not parse " + type + " '" + contents + "' (at row " + currentRow + ")");
	}
    }

    private static int parseID(String contents, int currentRow) throws DeserializingException {
	if (StringUtils.isBlank(contents)) {
	    return -1;
	}

	try {
	    return Integer.parseUnsignedInt(contents);
	} catch (NumberFormatException ex) {
	    throw new DeserializingException("Could not parse prompt ID '" + contents + "' (at row " + currentRow + ")");
	}
    }

// </editor-fold>
    
    public Dialogue load(String name) throws NullPointerException, FileNotFoundException, IOException, BiffException {
	if (name == null) {
	    throw new NullPointerException("Dialogue name cannot be null");
	}

	File file = DialoguesCache.getExcelFile(name);
	if (!file.isFile()) {
	    throw new FileNotFoundException("Missing excel file for dialogue '" + name + "'");
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
	if (!canonicalPath.endsWith(name.replace('.', File.separatorChar).concat(".xls"))) {
	    throw new FileNotFoundException("Missing excel file for dialogue '" + name + "'");
	}

	return load0(name, file);
    }

    private Dialogue load0(String name, File file) throws IOException, BiffException {
	Workbook workbook = Workbook.getWorkbook(file);

	if (workbook.getNumberOfSheets() == 0) {
	    throw new DeserializingException("This excel file doesn't contain any sheet!");
	}

	Sheet sheet = workbook.getSheet(0);

	return load0(name, sheet);
    }

    private Dialogue load0(String name, Sheet sheet) throws DeserializingException {
	LoadingData data = new LoadingData();

	for (int rowID = 0; rowID < sheet.getRows(); ++rowID) {
	    data.currentRow = rowID;
	    Cell[] row = sheet.getRow(rowID);

	    if (row.length < 2) {
		continue;
	    }

	    String identifier = row[0].getContents().toLowerCase();
	    switch (identifier) {
		case PREFIX_VAR:
		    parsePrefixes(row, data);
		    break;
		case START_VAR:
		    parseStart(row, data);
		    break;
		case PROMPT_IDENTIFIER:
		    parsePrompt(row, data);
	    }
	}

	if (data.firstPromptID == null) {
	    throw new DeserializingException("Missing 'START' variable");
	}

	// Last step: creating Dialogue and linking prompts to it
	Dialogue dialogue = new Dialogue(name, data.firstPromptID, data.prompts, data.prefixes);
	for (DialoguePrompt prompt : data.prompts.values()) {
	    prompt.init(dialogue);
	}
	return dialogue;
    }

    private void parsePrefixes(Cell[] row, LoadingData data) throws DeserializingException {
	if (data.prefixes != null) {
	    throw new DeserializingException("Variable 'PREFIXES' has been defined twice (at row " + data.currentRow + ")");
	}

	if (row.length < 2) {
	    throw new DeserializingException("Missing value(s) of 'PREFIXES' variable (at row " + data.currentRow + ")");
	}

	Map<Character, String> prefixes = new HashMap<>();
	Splitter splitter = Splitter.on('=').limit(2).omitEmptyStrings();
	for (int column = 1; column < row.length; ++column) {
	    String contents = row[column].getContents();
	    if (StringUtils.isEmpty(contents)) {
		break;
	    }

	    List<String> parts = splitter.splitToList(contents);
	    if (parts.size() != 2) {
		throw new DeserializingException(malformedPrefix(contents, data.currentRow, column));
	    }

	    String keyStr = parts.get(0);
	    if (keyStr.length() != 1) {
		throw new DeserializingException(malformedPrefix(contents, data.currentRow, column));
	    }

	    char key = keyStr.charAt(0);
	    String value = parts.get(1);

	    if (prefixes.put(key, value) != null) {
		throw new DeserializingException("Prefix '" + key + "' already present (at row " + data.currentRow + " and column " + column + ")");
	    }
	}

	data.prefixes = prefixes;
    }

    private void parseStart(Cell[] row, LoadingData data) throws DeserializingException {
	if (data.firstPromptID != null) {
	    throw new DeserializingException("Variable 'START' has been defined twice (at row " + data.currentRow + ")");
	}

	if (row.length < 2) {
	    throw new DeserializingException("Missing value of 'START' variable (at row " + data.currentRow + ")");
	}

	String contents = row[1].getContents();
	try {
	    data.firstPromptID = Integer.parseUnsignedInt(contents);
	} catch (NumberFormatException ex) {
	    throw new DeserializingException("Could not parse 'START' variable '" + contents + "' (at row " + data.currentRow + ")");
	}
    }

    private void parsePrompt(Cell[] row, LoadingData data) throws DeserializingException {
	int currentRow = data.currentRow;
	int promptID = currentRow + 1;

	if (row.length < 2) {
	    throw new DeserializingException("Prompt cannot be empty (at row " + currentRow + ")");
	}

	String message = getContents(row, Columns.MESSAGE);

	int initialDelay = parsePositive(getContents(row, Columns.INITIAL_DELAY), currentRow, "initial");

	int finalDelay = parsePositive(getContents(row, Columns.FINAL_DELAY), currentRow, "final");

	int nextPromptID = parseID(getContents(row, Columns.NEXT_PROMPT), currentRow);

	// prompt doesn't offer any choices
	if (row.length <= Columns.CHOICES_START) {
	    DialoguePrompt prompt = new DialoguePrompt(message, promptID, nextPromptID, initialDelay, finalDelay);
	    data.prompts.put(promptID, prompt);
	    return;
	}

	// prompt offers choices
	int timeout = parsePositive(getContents(row, Columns.TIMEOUT), currentRow, "timeout");

	List<InputChoice> choices = new ArrayList<>(5);
	int choiceID = 0;
	for (int i = Columns.CHOICES_START; i < row.length; i += Columns.CHOICE_LENGTH) {
	    ++choiceID;

	    String displayMessage = getContents(row, i);
	    String chatMessage = getContents(row, i + 1);
	    String nextPromptStr = getContents(row, i + 2);

	    if (StringUtils.isBlank(displayMessage) && StringUtils.isBlank(chatMessage) && StringUtils.isBlank(nextPromptStr)) {
		break;
	    }

	    int choiceNextPromptID = parseID(nextPromptStr, currentRow);
	    choices.add(new InputChoice(choiceID, choiceNextPromptID, displayMessage, chatMessage));
	}

	DialoguePrompt prompt = new DialoguePrompt(message, promptID, nextPromptID, initialDelay, finalDelay, timeout, choices);
	data.prompts.put(promptID, prompt);
    }

    private static class LoadingData {

	int currentRow;

	Integer firstPromptID;
	Map<Character, String> prefixes;
	Map<Integer, DialoguePrompt> prompts = new HashMap<>();
    }

    private static final class Columns {

	static final int MESSAGE = 1;

	static final int INITIAL_DELAY = 3;
	static final int FINAL_DELAY = 4;
	static final int NEXT_PROMPT = 5;

	static final int TIMEOUT = 7;
	static final int CHOICES_START = 9;
	static final int CHOICE_LENGTH = 4; // 3 data + 1 empty

	private Columns() {
	    throw new AssertionError();
	}
    }
}
