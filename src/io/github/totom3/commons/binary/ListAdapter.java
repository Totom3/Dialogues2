package io.github.totom3.commons.binary;

import java.io.IOException;
import java.util.List;

/**
 *
 * @author Totom3
 * @param <T>
 */
public abstract class ListAdapter<T> implements BinaryAdapter<List<T>> {

    @Override
    public List<T> read(DeserializationContext context) throws IOException {
	int length = context.readInt();

	List<T> list = supply(length);

	for (int i = 0; i < length; ++i) {
	    list.add(readElement(context));
	}

	return list;
    }

    @Override
    public void write(List<T> list, SerializationContext context) throws IOException {
	// Write list length
	context.writeInt(list.size());

	// Write elements
	for (T elem : list) {
	    writeElement(elem, context);
	}
    }

    protected abstract List<T> supply(int length);

    protected abstract void writeElement(T obj, SerializationContext context);

    protected abstract T readElement(DeserializationContext context);
}
