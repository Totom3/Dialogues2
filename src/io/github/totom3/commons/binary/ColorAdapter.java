package io.github.totom3.commons.binary;

import java.io.IOException;
import org.bukkit.Color;

/**
 *
 * @author Totom3
 */
public class ColorAdapter implements BinaryAdapter<Color> {

    @Override
    public Color read(DeserializationContext context) throws IOException {
	try {
	    return Color.fromRGB(context.readShort(), context.readShort(), context.readShort());
	} catch (IllegalArgumentException ex) {
	    throw new DeserializingException("Could not construct color", ex);
	}
    }

    @Override
    public void write(Color obj, SerializationContext context) throws IOException {
	context.writeShort(obj.getRed());
	context.writeShort(obj.getGreen());
	context.writeShort(obj.getBlue());
    }

}
