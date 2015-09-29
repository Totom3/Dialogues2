package io.github.totom3.commons.chat;

import io.github.totom3.commons.binary.BinaryAdapter;
import io.github.totom3.commons.binary.DeserializationContext;
import io.github.totom3.commons.binary.DeserializingException;
import io.github.totom3.commons.binary.SerializationContext;
import io.github.totom3.commons.chat.ChatComponent.Flags;
import java.io.IOException;
import java.util.List;
import org.bukkit.ChatColor;

/**
 *
 * @author Totom3
 */
public class ChatComponentAdapter implements BinaryAdapter<ChatComponent> {

    @Override
    public ChatComponent read(DeserializationContext context) throws IOException {
	// Read empty flag
	byte readByte = context.readByte();
	switch (readByte) {
	    case 0:
		break;
	    case 1:
		return new ChatComponent();
	    default:
		throw new DeserializingException("Unexpected ChatComponent empty flag: " + readByte);
	}

	// Read text
	String text = context.readString();

	// Read flags
	Flags f = new Flags(context.readByte(), context.readByte());

	// Read color
	ChatColor color = null;
	byte ordinal = context.readByte();
	if (ordinal != 0) {
	    ChatColor[] values = ChatColor.values();
	    ordinal--;
	    if (ordinal < 0 || ordinal >= values.length) {
		throw new DeserializingException("Read invalid ChatColor ordinal: expected from 0 (inclusive) to " + values.length + "(exclusive); got instead " + ordinal);
	    }
	    color = values[ordinal];
	    if (!color.isColor()) {
		throw new DeserializingException("Expected color ChatColor, but got instead " + color);
	    }
	}

	// Read insertion
	String insertion = context.readString();

	// Read chat click event
	ChatClickEvent clickEvent = null;
	ChatHoverEvent hoverEvent = null;

	ordinal = context.readByte();
	if (ordinal != 0) {
	    ordinal--;
	    // Read action
	    ChatClickAction[] values = ChatClickAction.values();
	    if (ordinal < 0 || ordinal >= values.length) {
		throw new DeserializingException("Read invalid ChatClickAction ordinal: expected from 0 (inclusive) to " + values.length + "(exclusive); got instead " + ordinal);
	    }
	    // Read value 
	    clickEvent = new ChatClickEvent(
		    values[ordinal],
		    context.readString()
	    );
	}

	// Read chat hover event
	ordinal = context.readByte();
	if (ordinal != 0) {
	    ordinal--;
	    // Read action
	    ChatHoverAction[] values = ChatHoverAction.values();
	    if (ordinal < 0 || ordinal >= values.length) {
		throw new DeserializingException("Read invalid ChatHoverAction ordinal: expected from 0 (inclusive) to " + values.length + "(exclusive); got instead " + ordinal);
	    }
	    // Read value 
	    hoverEvent = new ChatHoverEvent(
		    values[ordinal],
		    context.readObject(ChatComponent.class)
	    );
	}

	// Read children
	List<ChatComponent> children = context.readList(ChatComponent.class);

	ChatComponent comp = new ChatComponent()
		.setText(text)
		.setInsertion(insertion)
		.setColor(color)
		.setClickEvent(clickEvent)
		.setHoverEvent(hoverEvent)
		.setChilds(children);
	comp.flags = f;

	return comp;
    }

    @Override
    public void write(ChatComponent comp, SerializationContext context) throws IOException {
	if (comp.isEmpty()) {
	    context.writeByte(1);
	    return;
	} else {
	    context.writeByte(0);
	}

	// Write text
	context.writeString(comp.getText());

	Flags f = comp.flags;

	// Write defined flags
	context.writeByte(f.definedFlags);

	// Write actual flags
	context.writeByte(f.actualFlags);

	// Write color
	byte ordinal = (byte) ((comp.hasColor()) ? comp.getColor().ordinal() + 1 : 0);
	context.writeByte(ordinal);

	// Write insertion
	if (comp.hasInsertion()) {
	    context.writeString(comp.getInsertion());
	} else {
	    context.writeInt(-1);
	}

	// Write click event
	if (!comp.hasClickEvent()) {
	    context.writeByte(0);
	} else {
	    ChatClickEvent clickEvent = comp.getClickEvent();
	    context.writeByte(clickEvent.getAction().ordinal() + 1); // Write action
	    context.writeString(clickEvent.getValue()); // Write value
	}

	// Write hover event
	if (!comp.hasHoverEvent()) {
	    context.writeByte(0);
	} else {
	    ChatHoverEvent hoverEvent = comp.getHoverEvent();
	    context.writeByte(hoverEvent.getAction().ordinal() + 1); // Write action
	    context.writeObject(hoverEvent.getValue()); // Write value
	}

	context.writeCollection(comp.getChilds());
    }
}
