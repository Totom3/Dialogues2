package io.github.totom3.commons.binary;

import com.google.common.base.Preconditions;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author Totom3
 */
public class SerializationContext extends BinaryDataBank {

    private final BinaryIO binaryIO;
    private final DataOutput out;

    public SerializationContext(DataOutput out) {
	this.binaryIO = BinaryIO.get();
	this.out = Preconditions.checkNotNull(out);
    }

    public DataOutput out() {
	return out;
    }

    public <E extends Enum<E>> void writeEnum(E e) throws IOException {
	binaryIO.writeEnum(e, this);
    }

    public <T> void writeObject(T o) throws IOException {
	if (o == null) {
	    throw new DeserializingException("Cannot write null object");
	}

	binaryIO.writeObject(o, (Class<T>) o.getClass(), this);
    }

    public <T> void writeCollection(Collection<T> coll) throws IOException {
	binaryIO.writeCollection(coll, this);
    }

    public <K, V> void writeMap(Map<? extends K, ? extends V> map) throws IOException {
	binaryIO.writeMap(map, this);
    }

    public void writeString(String str) throws IOException {
	if (str == null) {
	    out.writeInt(-1);
	    return;
	}

	if (str.isEmpty()) {
	    out.writeInt(0);
	    return;
	}

	byte[] ba = str.getBytes(StandardCharsets.UTF_8);
	out.writeInt(ba.length);
	out.write(ba);
    }

    public void writeBoolean(boolean bool) throws IOException {
	out.writeByte((bool) ? 1 : 0);
    }

    public boolean writeAndReturnBool(boolean bool) throws IOException {
	writeBoolean(bool);
	return bool;
    }
    
    // ---------------[ Deleguate Methods ]---------------
    public void writeByte(int v) throws IOException {
	out.writeByte(v);
    }

    public void writeShort(int v) throws IOException {
	out.writeShort(v);
    }

    public void writeChar(int v) throws IOException {
	out.writeChar(v);
    }

    public void writeInt(int v) throws IOException {
	out.writeInt(v);
    }

    public void writeLong(long v) throws IOException {
	out.writeLong(v);
    }

    public void writeFloat(float v) throws IOException {
	out.writeFloat(v);
    }

    public void writeDouble(double v) throws IOException {
	out.writeDouble(v);
    }

}
