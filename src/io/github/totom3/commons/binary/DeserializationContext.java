package io.github.totom3.commons.binary;

import com.google.common.base.Preconditions;
import java.io.Closeable;
import java.io.DataInput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Totom3
 */
public class DeserializationContext extends BinaryDataBank implements Closeable {

    private final BinaryIO binaryIO;
    private final DataInput in;

    public DeserializationContext(DataInput in) {
	this.binaryIO = BinaryIO.get();
	this.in = Preconditions.checkNotNull(in);
    }

    public DataInput in() {
	return in;
    }

    public <E extends Enum<E>> E readEnum(Class<E> clazz) throws IOException {
	return binaryIO.readEnum(clazz, this);
    }

    public <T> T readObject(Class<T> clazz) throws IOException {
	return binaryIO.readObject(clazz, this);
    }

    public <T> List<T> readList(Class<T> clazz) throws IOException {
	return binaryIO.readList(clazz, this);
    }

    public <K, V> Map<K, V> readMap(Class<? extends K> keyClass, Class<? extends V> valueClass) throws IOException {
	return binaryIO.readMap(keyClass, valueClass, this);
    }

    public String readString() throws IOException {
	int length = in.readInt();
	if (length == -1) {
	    return null;
	}

	if (length == 0) {
	    return "";
	}

	byte[] ba = new byte[length];
	in.readFully(ba);

	return new String(ba, StandardCharsets.UTF_8);
    }

    public boolean readBoolean() throws IOException {
	byte b = in.readByte();
	if (b == 1) {
	    return true;
	} else if (b == 0) {
	    return false;
	} else {
	    throw new DeserializingException("Expected 1 or 0 for boolean; got instead " + b);
	}
    }

    @Override
    public void close() throws IOException {
	if (in instanceof Closeable) {
	    ((Closeable) in).close();
	}
    }

    // ---------------[ Deleguate Methods ]---------------
    public byte readByte() throws IOException {
	return in.readByte();
    }

    public int readUnsignedByte() throws IOException {
	return in.readUnsignedByte();
    }

    public short readShort() throws IOException {
	return in.readShort();
    }

    public int readUnsignedShort() throws IOException {
	return in.readUnsignedShort();
    }

    public char readChar() throws IOException {
	return in.readChar();
    }

    public int readInt() throws IOException {
	return in.readInt();
    }

    public long readLong() throws IOException {
	return in.readLong();
    }

    public float readFloat() throws IOException {
	return in.readFloat();
    }

    public double readDouble() throws IOException {
	return in.readDouble();
    }

}
