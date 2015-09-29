package io.github.totom3.commons.binary;

import io.github.totom3.commons.chat.ChatComponent;
import io.github.totom3.commons.chat.ChatComponentAdapter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Totom3
 */
public class ObjectAdapters {

    private static final IDBinaryAdapter<Object> adapter;

    static {
	adapter = IDBinaryAdapter.builder()
		// Primitive adapters
		.addAdapter(0, String.class, new StringAdapter())
		.addAdapter(1, Byte.class, new ByteAdapter())
		.addAdapter(2, Short.class, new ShortAdapter())
		.addAdapter(3, Integer.class, new IntAdapter())
		.addAdapter(4, Long.class, new LongAdapter())
		.addAdapter(5, Float.class, new FloatAdapter())
		.addAdapter(6, Double.class, new DoubleAdapter())
		.addAdapter(7, UUID.class, new UUIDAdapter())
		.addAdapter(8, Boolean.class, new BooleanAdapter())
		.addAdapter(9, Character.class, new CharAdapter())
		// Structures adapters
		.addAdapter(10, List.class, new ListAdapter())
		.addAdapter(11, Set.class, new SetAdapter())
		.addAdapter(12, Map.class, new MapAdapter())
		// Bukkit & Totom's adapters
		.addAdapter(13, ItemStack.class, new ItemStackAdapter())
		.addAdapter(14, Location.class, new LocationAdapter())
		.addAdapter(15, ChatComponent.class, new ChatComponentAdapter())
		.addAdapter(16, Enchantment.class, new EnchantmentAdapter())
		.build();
    }

    public static IDBinaryAdapter<Object> getAdapter() {
	return adapter;
    }

    private ObjectAdapters() {
    }

    static class ByteAdapter implements BinaryAdapter<Byte> {

	@Override
	public Byte read(DeserializationContext context) throws IOException {
	    return context.readByte();
	}

	@Override
	public void write(Byte obj, SerializationContext context) throws IOException {
	    context.writeByte(obj);
	}
    }

    static class ShortAdapter implements BinaryAdapter<Short> {

	@Override
	public Short read(DeserializationContext context) throws IOException {
	    return context.readShort();
	}

	@Override
	public void write(Short obj, SerializationContext context) throws IOException {
	    context.writeShort(obj);
	}
    }

    static class CharAdapter implements BinaryAdapter<Character> {

	@Override
	public Character read(DeserializationContext context) throws IOException {
	    return context.readChar();
	}

	@Override
	public void write(Character obj, SerializationContext context) throws IOException {
	    context.writeChar(obj);
	}
    }

    static class IntAdapter implements BinaryAdapter<Integer> {

	@Override
	public Integer read(DeserializationContext context) throws IOException {
	    return context.readInt();
	}

	@Override
	public void write(Integer obj, SerializationContext context) throws IOException {
	    context.writeInt(obj);
	}
    }

    static class LongAdapter implements BinaryAdapter<Long> {

	@Override
	public Long read(DeserializationContext context) throws IOException {
	    return context.readLong();
	}

	@Override
	public void write(Long obj, SerializationContext context) throws IOException {
	    context.writeLong(obj);
	}
    }

    static class FloatAdapter implements BinaryAdapter<Float> {

	@Override
	public Float read(DeserializationContext context) throws IOException {
	    return context.readFloat();
	}

	@Override
	public void write(Float obj, SerializationContext context) throws IOException {
	    context.writeFloat(obj);
	}
    }

    static class DoubleAdapter implements BinaryAdapter<Double> {

	@Override
	public Double read(DeserializationContext context) throws IOException {
	    return context.readDouble();
	}

	@Override
	public void write(Double obj, SerializationContext context) throws IOException {
	    context.writeDouble(obj);
	}
    }

    static class BooleanAdapter implements BinaryAdapter<Boolean> {

	@Override
	public Boolean read(DeserializationContext context) throws IOException {
	    byte b = context.readByte();
	    switch (b) {
		case 0:
		    return Boolean.FALSE;
		case 1:
		    return Boolean.TRUE;
		default:
		    throw new DeserializingException("Unexpected boolean flag: " + b);
	    }
	}

	@Override
	public void write(Boolean obj, SerializationContext context) throws IOException {
	    context.writeByte((obj) ? 1 : 0);
	}
    }

    static class ListAdapter implements BinaryAdapter<List> {

	@Override
	public List read(DeserializationContext context) throws IOException {
	    return context.readList(Object.class);
	}

	@Override
	public void write(List obj, SerializationContext context) throws IOException {
	    context.writeCollection(obj);
	}

    }

    static class SetAdapter implements BinaryAdapter<Set> {

	@Override
	public Set read(DeserializationContext context) throws IOException {
	    return new HashSet<>(context.readList(Object.class));
	}

	@Override
	public void write(Set obj, SerializationContext context) throws IOException {
	    context.writeCollection(obj);
	}

    }

    static class MapAdapter implements BinaryAdapter<Map> {

	@Override
	public Map read(DeserializationContext context) throws IOException {
	    return context.readMap(Object.class, Object.class);
	}

	@Override
	public void write(Map obj, SerializationContext context) throws IOException {
	    context.writeMap(obj);
	}
    }

    static class StringAdapter implements BinaryAdapter<String> {

	@Override
	public String read(DeserializationContext context) throws IOException {
	    return context.readString();
	}

	@Override
	public void write(String obj, SerializationContext context) throws IOException {
	    context.writeString(obj);
	}
    }

    static class UUIDAdapter implements BinaryAdapter<UUID> {

	@Override
	public UUID read(DeserializationContext context) throws IOException {
	    return new UUID(context.readLong(), context.readLong());
	}

	@Override
	public void write(UUID obj, SerializationContext context) throws IOException {
	    context.writeLong(obj.getMostSignificantBits());
	    context.writeLong(obj.getLeastSignificantBits());
	}
    }

}
