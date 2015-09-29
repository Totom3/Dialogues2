package io.github.totom3.commons.binary;

import java.io.IOException;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;

/**
 *
 * @author Totom3
 */
public class MaterialDataAdapter implements BinaryAdapter<MaterialData>{

    @Override
    public MaterialData read(DeserializationContext context) throws IOException {
	Material mat = context.readEnum(Material.class);
	byte data = context.readByte();
	
	return new MaterialData(mat, data);
    }

    @Override
    public void write(MaterialData data, SerializationContext context) throws IOException {
	context.writeEnum(data.getItemType());
	context.writeByte(data.getData());
    }

}
