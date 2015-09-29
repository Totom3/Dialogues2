package io.github.totom3.commons.binary;

import java.io.IOException;
import org.bukkit.enchantments.Enchantment;

/**
 *
 * @author Totom3
 */
public class EnchantmentAdapter implements BinaryAdapter<Enchantment> {

    @Override
    public Enchantment read(DeserializationContext context) throws IOException {
	byte readByte = context.readByte();
	Enchantment enchant = Enchantment.getById(readByte);
	if (enchant == null) {
	    throw new DeserializingException("No such Enchantment with ID " + readByte);
	}
	return enchant;
    }

    @Override
    public void write(Enchantment obj, SerializationContext context) throws IOException {
	context.writeByte(obj.getId());
    }

}
