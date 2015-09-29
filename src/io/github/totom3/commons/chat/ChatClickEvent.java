package io.github.totom3.commons.chat;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.base.Strings;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import net.minecraft.server.v1_8_R3.ChatClickable;
import net.minecraft.server.v1_8_R3.ChatClickable.EnumClickAction;

/**
 *
 * @author Totom3
 */
public class ChatClickEvent implements Cloneable {

    public static ChatClickable toNMS(ChatClickEvent event) {
	return new ChatClickable(
		EnumClickAction.valueOf(event.getAction().name()), 
		event.getValue()
	);
    }
    
    public static ChatClickEvent fromNMS(ChatClickable nmsEvent) {
	return new ChatClickEvent(ChatClickAction.valueOf(nmsEvent.a().name()), nmsEvent.b());
    }
    
    private final ChatClickAction action;
    private String value;

    public ChatClickEvent(ChatClickAction action, String value) {
	this.action = checkNotNull(action);
	this.value = Strings.nullToEmpty(value);
    }

    public ChatClickAction getAction() {
	return action;
    }

    public String getValue() {
	return value;
    }

    public void setValue(String value) {
	this.value = Strings.nullToEmpty(value);
    }
    
    @Override
    public ChatClickEvent clone() {
	return new ChatClickEvent(action, value);
    }
    
    static class ChatClickEventAdapter implements JsonDeserializer<ChatClickEvent>, JsonSerializer<ChatClickEvent> {
	@Override
	public JsonElement serialize(ChatClickEvent obj, Type ignored, JsonSerializationContext context) {
	    JsonObject json = new JsonObject();
	    
	    json.addProperty("action", obj.getAction().name().toLowerCase());
	    json.addProperty("value", obj.getValue());
	    
	    return json;
	}

	@Override
	public ChatClickEvent deserialize(JsonElement element, Type ignored, JsonDeserializationContext context) throws JsonParseException {
	    if (element.isJsonNull()) {
		return null;
	    }
	    
	    ChatClickAction action;
	    String value;
	    
	    JsonObject json = element.getAsJsonObject();
	    
	    JsonElement jsonElement = json.get("action");
	    if (jsonElement == null) {
		throw new JsonParseException("Missing action property in ChatClickEvent");
	    }
	    try {
		action = ChatClickAction.valueOf(jsonElement.getAsJsonPrimitive().getAsString());
	    } catch (IllegalArgumentException ex) {
		throw new JsonParseException("Could not parse action property: ", ex);
	    }
	    
	    jsonElement = json.get("value");
	    if (jsonElement == null) {
		throw new JsonParseException("Missing value property in ChatClickEvent");
	    }
	    
	    value = jsonElement.getAsJsonPrimitive().getAsString();
	    
	    return new ChatClickEvent(action, value);
	}
    }
}
