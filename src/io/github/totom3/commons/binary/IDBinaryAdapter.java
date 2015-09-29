package io.github.totom3.commons.binary;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Totom3
 * @param <T>
 */
public class IDBinaryAdapter<T> implements BinaryAdapter<T> {

    public static <E> Builder<E> builder() {
	return new Builder<>();
    }

    private final TokenType token;
    private final BinaryAdapter<T> fallbackAdapter;
    private final BiMap<Integer, BinaryAdapter<? extends T>> adapters;
    private final Map<Class<? extends T>, BinaryAdapter<? extends T>> adaptersByClass;

    private IDBinaryAdapter(TokenType type, BinaryAdapter<T> fallback, BiMap<Integer, BinaryAdapter<? extends T>> adapters, Map<Class<? extends T>, BinaryAdapter<? extends T>> adaptersByClass) {
	this.token = type;
	this.fallbackAdapter = fallback;
	this.adapters = HashBiMap.create(adapters);
	this.adaptersByClass = new HashMap<>(adaptersByClass);
    }

    // Read by ID
    @Override
    public T read(DeserializationContext context) throws IOException {
	int readID = token.read(context.in());
	BinaryAdapter<? extends T> adapter = adapters.get(readID);
	if (adapter == null) {
	    if ((adapter = fallbackAdapter) == null) {
		throw new DeserializingException("Missing BinaryAdapter for ID " + readID);
	    }
	}

	return adapter.read(context);
    }

    // Write by class
    @Override
    public void write(T obj, SerializationContext context) throws IOException {
	Class<? extends T> clazz = (Class<? extends T>) obj.getClass();

	BinaryAdapter<T> adapter = (BinaryAdapter<T>) adaptersByClass.get(clazz);
	int id;
	if (adapter == null) {
	    if (fallbackAdapter == null) {
		throw new SerializingException("Missing BinaryAdapter for class " + clazz);
	    }
	    adapter = fallbackAdapter;
	    id = -1;
	} else {
	    id = adapters.inverse().get(adapter);
	}
	token.write(context.out(), id);

	adapter.write(obj, context);
    }

    public static enum TokenType {

	BYTE {

		    @Override
		    int read(DataInput in) throws IOException {
			return in.readByte();
		    }

		    @Override
		    void write(DataOutput out, int i) throws IOException {
			out.writeByte(i);
		    }
		},

	SHORT {

		    @Override
		    int read(DataInput in) throws IOException {
			return in.readShort();
		    }

		    @Override
		    void write(DataOutput out, int i) throws IOException {
			out.writeShort(i);
		    }
		},

	INTEGER {

		    @Override
		    int read(DataInput in) throws IOException {
			return in.readInt();
		    }

		    @Override
		    void write(DataOutput out, int i) throws IOException {
			out.writeInt(i);
		    }
		};

	abstract int read(DataInput in) throws IOException;

	abstract void write(DataOutput out, int i) throws IOException;
    }

    public static class Builder<E> {

	private TokenType token = TokenType.BYTE;
	private BinaryAdapter<E> fallback;
	private final BiMap<Integer, BinaryAdapter<? extends E>> adapters = HashBiMap.create();
	private final Map<Class<? extends E>, BinaryAdapter<? extends E>> adaptersByClass = new HashMap<>();

	private Builder() {
	}

	public <E2 extends E> Builder<E> addAdapter(int id, Class<E2> clazz, BinaryAdapter<E2> adapter) {
	    checkNotNull(adapter, "Cannot add null adapter");
	    checkNotNull(clazz, "Class cannot be null");

	    if (id == -1) {
		throw new IllegalArgumentException("-1 ID is used for fallback adapter");
	    }

	    if (adapters.putIfAbsent(id, adapter) != null) {
		throw new IllegalArgumentException("Adapter for ID " + id + " was already set.");
	    }

	    adaptersByClass.put(clazz, adapter);

	    return this;
	}

	public Builder<E> withFallbackAdapter(BinaryAdapter<E> fallback) {
	    this.fallback = fallback;
	    return this;
	}

	public Builder<E> withTokenType(TokenType token) {
	    this.token = checkNotNull(token);
	    return this;
	}

	public IDBinaryAdapter<E> build() {
	    return new IDBinaryAdapter<>(token, fallback, adapters, adaptersByClass);
	}
    }
}
