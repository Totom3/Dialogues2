package io.github.totom3.commons.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import io.github.totom3.commons.chat.ChatMessageSender.ChatMessageType;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.RandomAccess;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.amoebaman.amoebautils.nms.ReflectionUtil;
import net.minecraft.server.v1_8_R3.ChatClickable;
import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.ChatHoverable;
import net.minecraft.server.v1_8_R3.ChatModifier;
import net.minecraft.server.v1_8_R3.EnumChatFormat;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 *
 * @author Totom3
 */
public class ChatComponent implements Cloneable {

    static final Gson GSON = new GsonBuilder()
	    .registerTypeAdapter(ChatComponent.class, new ChatComponentAdapter())
	    .registerTypeAdapter(ChatClickEvent.class, new ChatClickEvent.ChatClickEventAdapter())
	    .registerTypeAdapter(ChatHoverEvent.class, new ChatHoverEvent.ChatHoverEventAdapter())
	    .create();

    public static final byte BOLD = 0x1;
    public static final byte ITALIC = 0x2;
    public static final byte UNDERLINED = 0x4;
    public static final byte STRIKE_THROUGH = 0x8;
    public static final byte OBFUSCATED = 0x10;

    private static final ChatComponent SUPER_PARENT = new ChatComponent() {

	@Override
	protected Optional<Boolean> getFlag(byte modif) {
	    return Optional.of(false);
	}

	@Override
	protected void setFlag(byte modif, Boolean value) {
	}

	@Override
	public ChatComponent clone() {
	    throw new UnsupportedOperationException();
	}
    };

    private static Pattern COLOR_PATTERN = Pattern.compile("(" + ChatColor.COLOR_CHAR + "[0-9a-fk-or]" + ")");

    private static ChatComponent maskNullParent(ChatComponent parent) {
	if (parent == null) {
	    return SUPER_PARENT;
	}

	return parent;
    }

    private static ChatComponent unmaskNullParent(ChatComponent parent) {
	if (parent == SUPER_PARENT) {
	    return null;
	}

	return parent;
    }

    public static String toJson(ChatComponent comp) {
	return GSON.toJson(comp);
    }

    public static ChatComponent fromJson(String str) throws JsonParseException {
	return GSON.fromJson(str, ChatComponent.class);
    }

    public static ChatComponent fromNMS(IChatBaseComponent nmsComp) {
	ChatModifier modif = nmsComp.getChatModifier();

	ChatComponent comp = new ChatComponent()
		.setText(nmsComp.getText())
		.setBold((Boolean) getModifier(modif, "c"))
		.setItalic((Boolean) getModifier(modif, "d"))
		.setUnderlined((Boolean) getModifier(modif, "e"))
		.setStrikeThrough((Boolean) getModifier(modif, "f"))
		.setObfuscated((Boolean) getModifier(modif, "g"))
		.setInsertion((String) getModifier(modif, "j"));

	Object field = getModifier(modif, "h");
	if (field != null) {
	    comp.setClickEvent(ChatClickEvent.fromNMS((ChatClickable) field));
	}

	field = getModifier(modif, "i");
	if (field != null) {
	    comp.setHoverEvent(ChatHoverEvent.fromNMS((ChatHoverable) field));
	}

	field = getModifier(modif, "b");
	if (field != null) {
	    comp.setColor(ChatColor.valueOf(((Enum) field).name()));
	}

	// Children
	List<ChatComponent> children = comp.getChilds();
	for (IChatBaseComponent nmsChild : nmsComp.a()) {
	    ChatComponent child = fromNMS(nmsChild);
	    child.setParent(comp);
	    children.add(child);
	}

	return comp;
    }

    private static void appendFormat(ChatColor color, Flags flags, StringBuilder builder) {
	if (color != null) {
	    builder.append(color);
	}

	Optional<Boolean> flag;

	flag = flags.getFlag(BOLD);
	if (flag.isPresent() && flag.get() == true) {
	    builder.append(ChatColor.BOLD);
	}

	flag = flags.getFlag(ITALIC);
	if (flag.isPresent() && flag.get() == true) {
	    builder.append(ChatColor.ITALIC);
	}

	flag = flags.getFlag(UNDERLINED);
	if (flag.isPresent() && flag.get() == true) {
	    builder.append(ChatColor.UNDERLINE);
	}

	flag = flags.getFlag(STRIKE_THROUGH);
	if (flag.isPresent() && flag.get() == true) {
	    builder.append(ChatColor.STRIKETHROUGH);
	}

	flag = flags.getFlag(OBFUSCATED);
	if (flag.isPresent() && flag.get() == true) {
	    builder.append(ChatColor.MAGIC);
	}
    }

    public static ChatComponent fromPlainText(String text) {
	Matcher matcher = COLOR_PATTERN.matcher(text);

	ChatComponent root = new ChatComponent();
	ChatComponent previous = root;

	int previousStart = 0;

	while (matcher.find()) {
	    int start = matcher.start();
	    int end = matcher.end();

	    ChatColor color = ChatColor.getByChar(text.charAt(start + 1));

	    if (previousStart + 2 >= start) { // Previous has no text, we can directly modify it
		apply(color, previous);
		previousStart = end;
	    } else { // Previous has text; need to create a new child
		ChatComponent child = new ChatComponent();
		previous.setText(text.substring(previousStart, start));
		previousStart = end;
		apply(color, child);

		previous.getChilds().add(child);
		previous = child;
	    }
	}

	if (previousStart < text.length() - 1) { // Still a message left after last color
	    previous.setText(text.substring(previousStart));
	}

	return root;
    }

    private static void apply(ChatColor color, ChatComponent comp) {
	if (color == ChatColor.RESET) {
	    comp.negateAllFormats();
	} else if (color.isFormat()) {
	    comp.addFormat(color);
	} else {
	    comp.setColor(color);
	}
    }

    private static Object getModifier(ChatModifier modif, String fieldName) {
	try {
	    Field field = ReflectionUtil.getField(modif.getClass(), fieldName);
	    if (field == null) {
		return null;
	    }
	    return field.get(modif);
	} catch (IllegalArgumentException | IllegalAccessException ex) {
	    Logger.getLogger(ChatComponent.class.getName()).log(Level.SEVERE, "Could not get field " + fieldName + " from class " + modif.getClass() + ": ", ex);
	    return null;
	}
    }

    Flags flags;

    private String text;
    private ChatColor color;
    private String insertion;
    private ChatComponent parent;
    private ChatClickEvent clickEvent;
    private ChatHoverEvent hoverEvent;
    private Childs childs;

    public ChatComponent() {
	this.flags = new Flags();
	this.childs = new Childs();
	this.text = "";
	this.parent = SUPER_PARENT;
    }

    public ChatComponent(String text) {
	this();
	setText(text);
    }

    // --------------------[ TO/FROM MINECRAFT IMPLEMENTATION ]--------------------
    public IChatBaseComponent toNMS() {
	IChatBaseComponent nmsComp = new ChatComponentText(this.getText());

	ChatModifier modif = new ChatModifier()
		.setBold(this.getModifier(BOLD))
		.setItalic(this.getModifier(ITALIC))
		.setUnderline(this.getModifier(UNDERLINED))
		.setStrikethrough(this.getModifier(STRIKE_THROUGH))
		.setRandom(this.getModifier(OBFUSCATED))
		.setInsertion(this.insertion);

	if (hoverEvent != null) {
	    modif.setChatHoverable(hoverEvent.toNMS());
	}

	if (clickEvent != null) {
	    modif.setChatClickable(ChatClickEvent.toNMS(clickEvent));
	}

	if (color != null) {
	    modif.setColor(EnumChatFormat.valueOf(color.name()));
	}

	nmsComp.setChatModifier(modif);

	for (ChatComponent child : this.getChilds()) {
	    nmsComp.addSibling(child.toNMS());
	}

	return nmsComp;
    }

    // --------------------[ TO/FROM PLAIN TEXT ]--------------------
    public String toPlainText() {
	StringBuilder builder = new StringBuilder(160);

	return toPlainText(builder, null).toString();
    }

    private StringBuilder toPlainText(StringBuilder builder, ChatComponent previous) {
	if (previous != null && !previous.flags.isBlank()) {
	    Flags localFlags = Flags.mergeFlags(parent.flags, this.flags);
	    appendFormat(getColor(), localFlags, builder);
	}
	builder.append(getText());

	Iterator<ChatComponent> it = getChilds().iterator();
	ChatComponent child;
	ChatComponent prevChild;

	if (!it.hasNext()) {
	    return builder;
	}

	child = it.next();
	child.toPlainText(builder, null);

	for (; it.hasNext();) {
	    prevChild = child;
	    child = it.next();
	    child.toPlainText(builder, prevChild);
	}

	return builder;
    }

    public void send(Player player) {
	ChatMessageSender.send(this, player);
    }

    public void send(Player player, ChatMessageType messageType) {
	ChatMessageSender.send(this, player, messageType);
    }

    // --------------------[ GETTERS ]--------------------
    public boolean isBold() {
	return getFlag(BOLD).orElseGet(parent::isBold);
    }

    public boolean isItalic() {
	return getFlag(ITALIC).orElseGet(parent::isItalic);
    }

    public boolean isStrikethrough() {
	return getFlag(STRIKE_THROUGH).orElseGet(parent::isStrikethrough);
    }

    public boolean isUnderlined() {
	return getFlag(UNDERLINED).orElseGet(parent::isUnderlined);
    }

    public boolean isObfuscated() {
	return getFlag(OBFUSCATED).orElseGet(parent::isObfuscated);
    }

    public String getText() {
	return text;
    }

    public ChatClickEvent getClickEvent() {
	return Optional.ofNullable(clickEvent).orElseGet(parent::getClickEvent);
    }

    public ChatHoverEvent getHoverEvent() {
	return Optional.ofNullable(hoverEvent).orElseGet(parent::getHoverEvent);
    }

    public String getInsertion() {
	return Optional.ofNullable(insertion).orElseGet(parent::getInsertion);
    }

    public ChatComponent getParent() {
	return unmaskNullParent(parent);
    }

    public ChatColor getColor() {
	return Optional.ofNullable(color).orElseGet(parent::getColor);
    }

    public List<ChatComponent> getChilds() {
	return childs; // Intentionally returns internal instance
    }

    public Boolean getModifier(byte modif) {
	return getFlag(modif).orElse(null);
    }

    public boolean hasFormat(byte format) {
	return getFlag(format).orElse(Boolean.FALSE);
    }

    public boolean hasColor() {
	return color != null;
    }

    public boolean hasInsertion() {
	return insertion != null;
    }

    public boolean hasClickEvent() {
	return clickEvent != null;
    }

    public boolean hasHoverEvent() {
	return hoverEvent != null;
    }

    public boolean isEmpty() {
	return text == null
		&& flags.isBlank()
		&& childs.isEmpty()
		&& color == null
		&& insertion == null
		&& clickEvent == null
		&& hoverEvent == null;
    }

    // --------------------[ SETTERS ]--------------------
    public ChatComponent setBold(Boolean bold) {
	setFlag(BOLD, bold);
	return this;
    }

    public ChatComponent setItalic(Boolean italic) {
	setFlag(ITALIC, italic);
	return this;
    }

    public ChatComponent setStrikeThrough(Boolean strikeThrough) {
	setFlag(STRIKE_THROUGH, strikeThrough);
	return this;
    }

    public ChatComponent setUnderlined(Boolean underlined) {
	setFlag(UNDERLINED, underlined);
	return this;
    }

    public ChatComponent setObfuscated(Boolean obfuscated) {
	setFlag(OBFUSCATED, obfuscated);
	return this;
    }

    public ChatComponent setText(String text) {
	this.text = text;
	return this;
    }

    public ChatComponent setInsertion(String insertion) {
	this.insertion = insertion;
	return this;
    }

    public ChatComponent setClickEvent(ChatClickEvent event) {
	this.clickEvent = event;
	return this;
    }

    public ChatComponent setHoverEvent(ChatHoverEvent event) {
	this.hoverEvent = event;
	return this;
    }

    public ChatComponent setParent(ChatComponent parent) {
	this.parent = maskNullParent(parent);
	return this;
    }

    public ChatComponent setColor(ChatColor color) {
	this.color = color;
	return this;
    }

    public ChatComponent setChilds(List<ChatComponent> childs) {
	this.childs = new Childs();
	this.childs.addAll(childs);
	return this;
    }

    public ChatComponent addFormat(ChatColor format) {
	if (format == ChatColor.RESET) {
	    negateAllFormats();
	    return this;
	}

	if (!format.isFormat()) {
	    throw new IllegalArgumentException();
	}

	setFormat(format, true);

	return this;
    }

    public ChatComponent removeFormat(ChatColor format) {
	if (!format.isFormat()) {
	    throw new IllegalArgumentException();
	}

	if (format == ChatColor.RESET) {
	    negateAllFormats();
	} else {
	    setFormat(format, false);
	}

	return this;
    }

    private void setFormat(ChatColor format, Boolean value) {
	switch (format) {
	    case BOLD:
		setBold(value);
		break;
	    case ITALIC:
		setItalic(value);
		break;
	    case UNDERLINE:
		setUnderlined(value);
		break;
	    case MAGIC:
		setObfuscated(value);
		break;
	    case STRIKETHROUGH:
		setStrikeThrough(value);
		break;
	    default:
		throw new AssertionError("No such ChatColor format " + format);
	}
    }

    public ChatComponent negateAllFormats() {
	flags.definedFlags = BOLD | ITALIC | UNDERLINED | STRIKE_THROUGH | OBFUSCATED;
	flags.actualFlags = 0;
	return this;
    }

    public ChatComponent addChild(ChatComponent child) {
	childs.add(child);
	return this;
    }

    // --------------------[ INTERNAL ACCESSORS ]--------------------
    protected Optional<Boolean> getFlag(byte modif) {
	return flags.getFlag(modif);
    }

    protected void setFlag(byte modif, Boolean value) {
	if (value == null) {
	    // Remove bit
	    flags.definedFlags &= ~modif;
	    flags.actualFlags &= ~modif;
	    return;
	}

	flags.definedFlags |= modif;

	// Apply value on bit mask
	if (!value) {
	    modif = (byte) ~modif;
	    flags.actualFlags &= modif;
	} else {
	    flags.actualFlags |= modif;
	}
    }

    @Override
    public int hashCode() {
	int hash = 5;
	hash = 83 * hash + Objects.hashCode(this.text);
	hash = 83 * hash + Objects.hashCode(this.flags);
	hash = 83 * hash + Objects.hashCode(this.color);
	hash = 83 * hash + Objects.hashCode(this.insertion);
	hash = 83 * hash + Objects.hashCode(this.clickEvent);
	hash = 83 * hash + Objects.hashCode(this.hoverEvent);
	hash = 83 * hash + Objects.hashCode(this.childs);
	return hash;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final ChatComponent other = (ChatComponent) obj;
	if (!Objects.equals(this.text, other.text)) {
	    return false;
	}
	if (!Objects.equals(this.flags, other.flags)) {
	    return false;
	}
	if (this.color != other.color) {
	    return false;
	}
	if (!Objects.equals(this.insertion, other.insertion)) {
	    return false;
	}
	if (!Objects.equals(this.clickEvent, other.clickEvent)) {
	    return false;
	}
	if (!Objects.equals(this.hoverEvent, other.hoverEvent)) {
	    return false;
	}
	return Objects.equals(this.childs, other.childs);
    }

    @Override
    public String toString() {
	return "ChatComponent{"
		+ "text=" + text
		+ ", flags=" + Integer.toBinaryString(flags.actualFlags)
		+ ", definedFlags=" + Integer.toBinaryString(flags.definedFlags)
		+ ", color=" + color
		+ ", insertion=" + insertion
		+ ", clickEvent=" + clickEvent
		+ ", hoverEvent=" + hoverEvent
		+ ", childs=" + childs
		+ '}';
    }

    @Override
    public ChatComponent clone() {
	ChatComponent clone = new ChatComponent();
	clone.text = text;
	clone.color = color;
	clone.flags = flags.clone();
	clone.parent = parent;
	clone.insertion = insertion;
	clone.hoverEvent = (hoverEvent == null) ? null : hoverEvent.clone();
	clone.clickEvent = (clickEvent == null) ? null : clickEvent.clone();

	Childs otherChilds = clone.childs;
	for (ChatComponent child : childs) {
	    otherChilds.add(child.clone());
	}

	return clone;
    }

    static class ChatComponentAdapter implements JsonSerializer<ChatComponent>, JsonDeserializer<ChatComponent> {

	private static void setFlag(ChatComponent comp, JsonObject json, String name, byte flag) {
	    JsonElement element = json.get(name);

	    if (element != null) {
		comp.setFlag(flag, element.getAsBoolean());
	    }
	}

	private static void addFlag(JsonObject json, ChatComponent obj, String name, byte flag) {
	    Optional<Boolean> opt = obj.getFlag(flag);
	    if (opt.isPresent()) {
		json.addProperty(name, opt.get());
	    }
	}

	@Override
	public JsonElement serialize(ChatComponent obj, Type ignored, JsonSerializationContext context) {
	    JsonObject json = new JsonObject();

	    json.addProperty("text", obj.text);

	    addFlag(json, obj, "bold", BOLD);
	    addFlag(json, obj, "italic", ITALIC);
	    addFlag(json, obj, "underlined", UNDERLINED);
	    addFlag(json, obj, "obfuscated", OBFUSCATED);
	    addFlag(json, obj, "strikethrough", STRIKE_THROUGH);

	    if (obj.insertion != null) {
		json.addProperty("insertion", obj.insertion);
	    }

	    if (obj.color != null) {
		json.addProperty("color", obj.color.name().toLowerCase());
	    }

	    if (obj.clickEvent != null) {
		json.add("clickEvent", context.serialize(obj.clickEvent));
	    }

	    if (obj.hoverEvent != null) {
		json.add("hoverEvent", context.serialize(obj.hoverEvent));
	    }

	    JsonArray childs = new JsonArray();
	    for (ChatComponent c : obj.getChilds()) {
		childs.add(context.serialize(c));
	    }
	    if (childs.size() != 0) {
		json.add("extra", childs);
	    }

	    return json;
	}

	@Override
	public ChatComponent deserialize(JsonElement element, Type ignored, JsonDeserializationContext context) throws JsonParseException {
	    if (element.isJsonNull()) {
		return null;

	    } else if (element.isJsonPrimitive()) {
		JsonPrimitive prim = element.getAsJsonPrimitive();
		return new ChatComponent().setText(prim.getAsString());

	    } else if (element.isJsonArray()) {
		ChatComponent comp = new ChatComponent();
		List<ChatComponent> childs = comp.getChilds();
		for (JsonElement elmnt : element.getAsJsonArray()) {
		    ChatComponent child = context.deserialize(elmnt, ChatComponent.class);
		    child.setParent(comp);
		    childs.add(child);
		}
	    }

	    JsonObject json = element.getAsJsonObject();

	    String text = json.get("text").getAsString();

	    ChatComponent comp = new ChatComponent().setText(text);

	    setFlag(comp, json, "bold", BOLD);
	    setFlag(comp, json, "italic", ITALIC);
	    setFlag(comp, json, "underlined", UNDERLINED);
	    setFlag(comp, json, "obfuscated", OBFUSCATED);
	    setFlag(comp, json, "strikethrough", STRIKE_THROUGH);

	    JsonElement jsonElement = json.get("color");
	    if (jsonElement != null) {
		ChatColor color;
		try {
		    color = ChatColor.valueOf(jsonElement.getAsString().toUpperCase());
		} catch (IllegalArgumentException ex) {
		    throw new JsonParseException("Could not parse color: ", ex);
		}
		comp.setColor(color);
	    }

	    jsonElement = json.get("insertion");
	    if (jsonElement != null) {
		comp.setInsertion(jsonElement.getAsString());
	    }

	    jsonElement = json.get("clickEvent");
	    if (jsonElement != null) {
		ChatClickEvent evnt = context.deserialize(jsonElement, ChatClickEvent.class);
		comp.setClickEvent(evnt);
	    }

	    jsonElement = json.get("hoverEvent");
	    if (jsonElement != null) {
		ChatHoverEvent evnt = context.deserialize(jsonElement, ChatHoverEvent.class);
		comp.setHoverEvent(evnt);
	    }

	    jsonElement = json.get("extra");
	    if (jsonElement != null) {
		List<ChatComponent> childs = comp.getChilds();
		for (JsonElement elmt : jsonElement.getAsJsonArray()) {
		    ChatComponent childComponent = context.deserialize(elmt, ChatComponent.class);
		    childComponent.setParent(comp);
		    childs.add(childComponent);
		}
	    }

	    return comp;
	}
    }

    static class Flags implements Cloneable {

	static Flags mergeFlags(Flags parent, Flags child) {
	    byte defined = (byte) (parent.definedFlags | child.definedFlags);
	    byte actual = (byte) (child.actualFlags | (parent.actualFlags & ~child.definedFlags));
	    return new Flags(defined, actual);
	}
	byte actualFlags; // for each bit: if 0= disabled, 1= enabled
	byte definedFlags; // for each bit: 0= flag not set, 1= flag set

	Flags() {

	}

	Flags(byte definedFlags, byte actualFlags) {
	    this.actualFlags = actualFlags;
	    this.definedFlags = definedFlags;
	}

	Optional<Boolean> getFlag(byte modif) {
	    if ((definedFlags & modif) == 0) {
		return Optional.empty();
	    }

	    return Optional.of((actualFlags & modif) != 0);
	}

	boolean isBlank() {
	    return definedFlags == 0;
	}

	@Override
	public int hashCode() {
	    int hash = 3;
	    hash = 17 * hash + this.actualFlags;
	    hash = 17 * hash + this.definedFlags;
	    return hash;
	}

	@Override
	public boolean equals(Object obj) {
	    if (obj == null) {
		return false;
	    }
	    if (getClass() != obj.getClass()) {
		return false;
	    }
	    final Flags other = (Flags) obj;
	    if (this.actualFlags != other.actualFlags) {
		return false;
	    }

	    return this.definedFlags == other.definedFlags;
	}

	@Override
	public Flags clone() {
	    return new Flags(definedFlags, actualFlags);
	}
    }

    private class Childs extends AbstractList<ChatComponent> implements RandomAccess {

	private final List<ChatComponent> actualList = new ArrayList<>(5);

	private void checkAndSetParent(ChatComponent comp) {
	    if (comp == null) {
		throw new NullPointerException();
	    }

	    if (comp == ChatComponent.this) {
		throw new IllegalArgumentException();
	    }

	    comp.setParent(ChatComponent.this);
	}

	@Override
	public int size() {
	    return actualList.size();
	}

	@Override
	public boolean contains(Object o) {
	    if (o == null || !(o instanceof ChatComponent)) {
		return false;
	    }

	    return actualList.contains(o);
	}

	@Override
	public boolean add(ChatComponent e) {
	    checkAndSetParent(e);
	    return actualList.add(e);
	}

	@Override
	public boolean remove(Object o) {
	    if (o == null || !(o instanceof ChatComponent)) {
		return false;
	    }

	    return actualList.remove(o);
	}

	@Override
	public void clear() {
	    actualList.clear();
	}

	@Override
	public ChatComponent get(int index) {
	    return actualList.get(index);
	}

	@Override
	public ChatComponent set(int index, ChatComponent comp) {
	    checkAndSetParent(comp);
	    return actualList.set(index, comp);
	}

	@Override
	public void add(int index, ChatComponent comp) {
	    checkAndSetParent(comp);
	    actualList.add(index, comp);
	}

	@Override
	public ChatComponent remove(int index) {
	    return actualList.remove(index);
	}

	@Override
	public List<ChatComponent> subList(int fromIndex, int toIndex) {
	    return actualList.subList(fromIndex, toIndex);
	}

    }
}
