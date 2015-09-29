package io.github.totom3.commons.binary;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Totom3
 * @param <T>
 */
public class VersioningBinaryAdapter<T> implements BinaryAdapter<T> {

    public static <T> Builder<T> builder() {
	return new Builder<>();
    }

    private final Map<Integer, BinaryAdapter<T>> adapters;
    private int savingVersion;

    public VersioningBinaryAdapter(int savingVersion, Map<Integer, BinaryAdapter<T>> adapters) {
	this.adapters = new HashMap<>(adapters);
	this.savingVersion = savingVersion;
    }

    public int getSavingVersion() {
	return savingVersion;
    }

    public void setSavingVersion(int savingVersion) {
	this.savingVersion = savingVersion;
    }

    @Override
    public T read(DeserializationContext context) throws IOException {
	int version = context.readInt();

	return loadingAdapter(version).read(context);
    }

    @Override
    public void write(T obj, SerializationContext context) throws IOException {
	BinaryAdapter<T> adapt = savingAdapter();

	// Write version
	context.writeInt(savingVersion);

	// Write object
	adapt.write(obj, context);
    }

    private BinaryAdapter<T> loadingAdapter(int version) throws SerializingException {
	BinaryAdapter<T> adapt = adapters.get(version);
	if (adapt == null) {
	    throw new SerializingException("Missing adapter for loading version " + version);
	}
	return adapt;
    }

    private BinaryAdapter<T> savingAdapter() throws DeserializingException {
	BinaryAdapter<T> adapt = adapters.get(savingVersion);
	if (adapt == null) {
	    throw new DeserializingException("Missing adapter for saving version " + savingVersion);
	}
	return adapt;
    }

    public static class Builder<T> {

	private final Map<Integer, BinaryAdapter<T>> adapters = new HashMap<>();

	private Builder() {
	}

	public Builder<T> addAdapter(int version, BinaryAdapter<T> adapter) {
	    if (adapters.put(version, adapter) != null) {
		throw new IllegalArgumentException("Adapter already set for version " + version);
	    }
	    return this;
	}
	
	public VersioningBinaryAdapter<T> build(int savingVersion) {
	    return new VersioningBinaryAdapter<>(savingVersion, adapters);
	}
    }
}
