package io.github.totom3.commons.chat;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import net.minecraft.server.v1_8_R3.ChatHoverable;
import net.minecraft.server.v1_8_R3.ChatHoverable.EnumHoverAction;

/**
 *
 * @author Totom3
 */
public class ChatHoverEvent implements Cloneable {

    public static ChatHoverEvent fromNMS(ChatHoverable nmsEvent) {
	return new ChatHoverEvent(
		ChatHoverAction.valueOf(nmsEvent.a().name()), 
		ChatComponent.fromNMS(nmsEvent.b())
	);
    }
    
    
    private final ChatHoverAction action;
    private ChatComponent value;
    
    public ChatHoverEvent(ChatHoverAction action, ChatComponent value) {
	this.action = checkNotNull(action);
	this.value = checkNotNull(value);
    }

    public ChatHoverable toNMS() {
	return new ChatHoverable(
		EnumHoverAction.valueOf(this.getAction().name()),
		this.getValue().toNMS()
	);
    }

    public ChatHoverAction getAction() {
	return action;
    }

    public ChatComponent getValue() {
	return value;
    }

    public void setValue(ChatComponent value) {
	this.value = checkNotNull(value);
    }
    
    @Override
    public ChatHoverEvent clone() {
	return new ChatHoverEvent(action, value.clone());
    }
    
    static class ChatHoverEventAdapter implements JsonSerializer<ChatHoverEvent>, JsonDeserializer<ChatHoverEvent> {

	@Override
	public JsonElement serialize(ChatHoverEvent obj, Type ignored, JsonSerializationContext context) {
	    JsonObject json = new JsonObject();
	    
	    json.addProperty("action", obj.getAction().name().toLowerCase());
	    json.add("value", context.serialize(obj.getValue()));
	    
	    return json;
	}

	@Override
	public ChatHoverEvent deserialize(JsonElement element, Type ignored, JsonDeserializationContext context) throws JsonParseException {
	    if (element.isJsonNull()) {
		return null;
	    }
	    
	    ChatHoverAction action;
	    ChatComponent value;
	    
	    JsonObject json = element.getAsJsonObject();
	    
	    JsonElement jsonElement = json.getAsJsonPrimitive("action");
	    if (jsonElement == null) {
		throw new JsonParseException("Missing action property in ChatHoverEvent");
	    }
	    try {
		action = ChatHoverAction.valueOf(jsonElement.getAsJsonPrimitive().getAsString());
	    } catch (IllegalArgumentException ex) {
		throw new JsonParseException("Could not parse action property: ", ex);
	    }
	    
	    jsonElement = json.getAsJsonObject("value");
	    if (jsonElement == null) {
		throw new JsonParseException("Missing value property in ChatHoverEvent");
	    }
	    
	    value = context.deserialize(jsonElement, ChatComponent.class);
	    
	    return new ChatHoverEvent(action, value);
	}
    }
}
