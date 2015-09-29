package io.github.totom3.commons.binary;

import java.io.IOException;

/**
 *
 * @author Totom3
 * @param <T>
 */
public interface BinaryAdapter<T> {
    
    T read(DeserializationContext context) throws IOException;
    
    void write(T obj, SerializationContext context) throws IOException;
}
