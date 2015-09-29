package io.github.totom3.commons.binary;

import com.google.common.base.Preconditions;
import static com.google.common.base.Preconditions.checkNotNull;
import io.github.totom3.commons.binary.ObjectAdapters.StringAdapter;
import io.github.totom3.commons.chat.ChatComponent;
import io.github.totom3.commons.chat.ChatComponentAdapter;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

/**
 *
 * @author Totom3
 */
public class BinaryIO {

    private static final Map<Class<?>, BinaryAdapter<?>> defaultAdapters;

    private static final BinaryIO instance = new BinaryIO();

    static {
	defaultAdapters = new HashMap<>(17);
	add(Location.class, new LocationAdapter());
	add(ChatComponent.class, new ChatComponentAdapter());
	add(Enchantment.class, new EnchantmentAdapter());
	add(ItemStack.class, new ItemStackAdapter());
	add(Inventory.class, new InventoryAdapter());
	add(Color.class, new ColorAdapter());
	add(MaterialData.class, new MaterialDataAdapter());
	//
	add(Object.class, ObjectAdapters.getAdapter());
	add(String.class, new StringAdapter());
	add(Byte.class, new ObjectAdapters.ByteAdapter());
	add(Short.class, new ObjectAdapters.ShortAdapter());
	add(Integer.class, new ObjectAdapters.IntAdapter());
	add(Long.class, new ObjectAdapters.LongAdapter());
	add(UUID.class, new ObjectAdapters.UUIDAdapter());
	add(Float.class, new ObjectAdapters.FloatAdapter());
	add(Double.class, new ObjectAdapters.DoubleAdapter());
	add(Boolean.class, new ObjectAdapters.BooleanAdapter());
	add(Character.class, new ObjectAdapters.CharAdapter());
	add(List.class, new ObjectAdapters.ListAdapter());
	add(Set.class, new ObjectAdapters.SetAdapter());
	add(Map.class, new ObjectAdapters.MapAdapter());

    }

    private static <T> void add(Class<T> clazz, BinaryAdapter<T> adapter) {
	defaultAdapters.put(clazz, adapter);
    }

    /**
     * Writes a {@code String} to an {@code DataOutput} by first writing the
     * length (as an integer), and then the bytes, as defined by
     * {@link String#getBytes()}, using the {@code UTF-8} encoding.
     * <p>
     * @param out the stream to write to
     * @param str the string to be written
     * <p>
     * @throws IOException if an I/O error occurs
     */
    public static void writeString(DataOutput out, String str) throws IOException {
	if (str == null) {
	    out.writeInt(-1);
	    return;
	} else if (str.isEmpty()) {
	    out.writeInt(0);
	    return;
	}

	out.writeInt(str.length());
	out.writeBytes(str);
    }

    public static void writeShortString(DataOutput out, String str) throws IOException {
	if (str == null) {
	    out.writeShort(-1);
	    return;
	} else if (str.isEmpty()) {
	    out.writeShort(0);
	    return;
	}

	if (str.length() > Short.MAX_VALUE) {
	    throw new IllegalArgumentException("String has too many characters to be encoded with short: " + str.length() + " > " + Short.MAX_VALUE);
	}

	out.writeShort(str.length());
	out.writeBytes(str);
    }

    public static void writeByteString(DataOutput out, String str) throws IOException {
	if (str == null) {
	    out.writeByte(-1);
	} else if (str.isEmpty()) {
	    out.writeByte(0);
	} else {
	    if (str.length() > Byte.MAX_VALUE) {
		throw new IllegalArgumentException("String has too many characters to be encoded with byte: " + str.length() + " > " + Byte.MAX_VALUE);
	    }

	    out.writeByte(str.length());
	    out.writeBytes(str);
	}
    }

    /**
     * Reads a {@code String} from an {@code DataInput}. The method will first
     * readObject an integer from the stream, which will define then length of
     * the string. Then, {@code n} bytes will be readObject and will be
     * converted to a {@code String}.
     * <p>
     * @param in the stream to readObject from
     * <p>
     * @return the {@code String} readObject.
     * <p>
     * @throws EOFException if the end of the stream is reached while reading
     *                      the length, or if less bytes are read than the
     *                      expected length.
     * @throws IOException  if an I/O error occurs.
     */
    public static String readString(DataInput in) throws IOException {
	return doReadString(in.readInt(), in);
    }

    public static String readShortString(DataInput in) throws IOException {
	return doReadString(in.readShort(), in);
    }

    public static String readByteString(DataInput in) throws IOException {
	return doReadString(in.readByte(), in);
    }

    private static String doReadString(int length, DataInput in) throws IOException {
	if (length == -1) {
	    return null;
	}

	if (length == 0) {
	    return "";
	}

	byte bytes[] = new byte[length];
	in.readFully(bytes);

	return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Creates a new {@code BinaryIO} get.
     * <p>
     * @return
     */
    public static BinaryIO get() {
	return instance;
    }

    private final Map<Class<?>, BinaryAdapter<?>> adapters;

    private BinaryIO() {
	this.adapters = new HashMap<>();

    }

    public <T> BinaryAdapter<T> getReadAdapter(Class<T> clazz) {
	checkNotNull(clazz, "Clazz cannot be null.");

	if (clazz.isEnum()) {
	    return (BinaryAdapter<T>) new EnumAdapter<>((Class<? extends Enum>) clazz);
	}

	BinaryAdapter<?> adapter = defaultAdapters.get(clazz);
	if (adapter == null) {
	    adapter = adapters.get(clazz);
	}
	return (BinaryAdapter<T>) adapter;
    }

    public <T> BinaryAdapter<? super T> getWriteAdapter(Class<T> clazz) {
	checkNotNull(clazz, "Class cannot be null");

	if (clazz.isEnum()) {
	    return (BinaryAdapter<T>) new EnumAdapter<>((Class<? extends Enum>) clazz);
	}

	BinaryAdapter<? super T> adapter = getWriteAdapter(clazz, defaultAdapters);
	if (adapter == null || adapter == ObjectAdapters.getAdapter()) {
	    adapter = getWriteAdapter(clazz, adapters);
	}
	return adapter;
    }

    private <T> BinaryAdapter<? super T> getWriteAdapter(final Class<T> clazz, Map<Class<?>, BinaryAdapter<?>> map) {
	Class<? super T> c = clazz;

	BinaryAdapter<?> adapt;
	BinaryAdapter<?> interfaceAdapt;

	while (c != null) {

	    // get by (super) class
	    adapt = map.get(c);
	    if (adapt != null) {
		return (BinaryAdapter<? super T>) adapt;
	    }

	    // get by interface(s)
	    interfaceAdapt = getWriteAdapterFromInterfaces(c, map);
	    if (interfaceAdapt != null) {
		return (BinaryAdapter<? super T>) interfaceAdapt;
	    }
	    c = c.getSuperclass();
	}

	return null;
    }

    private <T> BinaryAdapter<? super T> getWriteAdapterFromInterfaces(Class<? super T> c, Map<Class<?>, BinaryAdapter<?>> map) {
	BinaryAdapter<?> adapter = map.get(c);

	if (adapter != null) {
	    return (BinaryAdapter<? super T>) adapter;
	}

	for (Class<? super T> interf : (Class<? super T>[]) c.getInterfaces()) {
	    adapter = map.get(interf);
	    if (adapter != null) {
		return (BinaryAdapter<? super T>) adapter;
	    }

	    adapter = getWriteAdapterFromInterfaces(interf, map);
	    if (adapter != null) {
		return (BinaryAdapter<? super T>) adapter;
	    }
	}

	return null;
    }

    public Map<Class<?>, BinaryAdapter<?>> getAllAdapters() {
	return Collections.unmodifiableMap(adapters);
    }

    public <T> BinaryIO registerAdapter(Class<T> c, BinaryAdapter<? super T> adapter) {
	Preconditions.checkNotNull(c, "Class cannot be null");
	Preconditions.checkNotNull(adapter, "Adapter cannot be null");

	adapters.put(c, adapter);

	return this;
    }

    // --------------------------[ Object IO ]--------------------------
    <T> T readObject(Class<T> clazz, DeserializationContext context) throws IOException {
	BinaryAdapter<T> adapt = getReadAdapter(clazz);
	if (adapt == null) {
	    throw new DeserializingException("Cannot readnpcd  object of type " + clazz.getName() + ": missing BinaryAdapter.");
	}

	return adapt.read(context);
    }

    <T> void writeObject(T object, Class<T> clazz, SerializationContext context) throws IOException {
	BinaryAdapter<? super T> adapter = getWriteAdapter(clazz);
	if (adapter == null) {
	    throw new SerializingException("Cannot write object " + object + ": no BinaryAdapter set for " + clazz);
	}

	adapter.write(object, context);
    }

    // --------------------------[ Enum IO ]--------------------------
    <E extends Enum<E>> E readEnum(Class<E> clazz, DeserializationContext context) throws IOException {
	short ordinal = context.readShort();
	E[] enumConstants = clazz.getEnumConstants();

	if (ordinal < 0 || ordinal >= enumConstants.length) {
	    throw new DeserializingException("Read invalid enum ordinal. Expected from 0 (inclusive) to " + enumConstants.length + " (exclusive). Got instead: " + ordinal);
	}

	return enumConstants[ordinal];
    }

    <E extends Enum<E>> void writeEnum(E e, SerializationContext context) throws IOException {
	context.writeShort(e.ordinal());
    }

    // --------------------------[ List IO ]--------------------------
    <T> List<T> readList(Class<T> clazz, DeserializationContext context) throws IOException {
	BinaryAdapter<T> adapt = getReadAdapter(clazz);
	if (adapt == null) {
	    throw new DeserializingException("Cannot read list of type " + clazz.getName() + ": missing BinaryAdapter.");
	}

	List<T> list = new ArrayList<>();

	// Read length
	int length = context.readInt();
	if (length == -1) {
	    return null;
	}

	// Read elements
	for (int i = 0; i < length; ++i) {
	    list.add(adapt.read(context));
	}

	return list;
    }

    <T> void writeCollection(Collection<? extends T> coll, SerializationContext context) throws IOException {
	if (coll == null) {
	    context.writeInt(-1);
	    return;
	}

	// Write size
	context.writeInt(coll.size());

	// Write elements
	for (T obj : coll) {
	    Class<?> clazz = obj.getClass();
	    BinaryAdapter<? super T> adapter = getWriteAdapter((Class<T>) clazz);
	    if (adapter == null) {
		throw new SerializingException("Cannot write object  of type " + clazz.getName() + ": missing BinaryAdapter.");
	    }

	    adapter.write(obj, context);
	}
    }

    // --------------------------[ Map IO ]--------------------------
    <K, V> Map<K, V> readMap(Class<? extends K> keyClass, Class<? extends V> valueClass, DeserializationContext context) throws IOException {
	// Get adapters
	BinaryAdapter<? extends K> keyAdapter;
	BinaryAdapter<? extends V> valueAdapter;

	keyAdapter = getWriteAdapter((Class) keyClass);
	if (keyAdapter == null) {
	    throw new DeserializingException("Cannot write map of type <" + keyClass.getName() + ", " + valueClass.getName() + ">: missing BinaryAdapter for " + keyClass);
	}

	valueAdapter = getWriteAdapter((Class) valueClass);
	if (valueAdapter == null) {
	    throw new DeserializingException("Cannot write map of type <" + keyClass.getName() + ", " + valueClass.getName() + ">: missing BinaryAdapter for " + valueClass);
	}

	// Read size
	int size = context.readInt();
	if (size < 0) {
	    throw new DeserializingException("Map size cannot be negative");
	}

	// Read keys and values
	Map<K, V> map = new HashMap<>(size);
	for (int i = 0; i < size; ++i) {
	    map.put(keyAdapter.read(context), valueAdapter.read(context));
	}

	return map;
    }

    <K, V> void writeMap(Map<? extends K, ? extends V> map, SerializationContext context) throws IOException {

	// Write size
	context.writeInt(map.size());

	// Write keys and values
	for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
	    K key = entry.getKey();
	    V value = entry.getValue();

	    // Get adapters
	    BinaryAdapter<? super K> keyAdapter;
	    BinaryAdapter<? super V> valueAdapter;

	    keyAdapter = getWriteAdapter((Class<K>) key.getClass());
	    if (keyAdapter == null) {
		throw new SerializingException("Cannot write key of type " + key.getClass().getName() + " from map: no BinaryAdapter set.");
	    }

	    valueAdapter = getWriteAdapter((Class<V>) value.getClass());
	    if (valueAdapter == null) {
		throw new SerializingException("Cannot write value of type " + value.getClass().getName() + " from map: no BinaryAdapter set.");
	    }

	    keyAdapter.write(entry.getKey(), context);
	    valueAdapter.write(entry.getValue(), context);
	}
    }

    class EnumAdapter<E extends Enum<E>> implements BinaryAdapter<E> {

	final Class<E> clazz;

	EnumAdapter(Class<E> clazz) {
	    this.clazz = clazz;
	}

	@Override
	public E read(DeserializationContext context) throws IOException {
	    return readEnum(clazz, context);
	}

	@Override
	public void write(E e, SerializationContext context) throws IOException {
	    writeEnum(e, context);
	}

    }
}
