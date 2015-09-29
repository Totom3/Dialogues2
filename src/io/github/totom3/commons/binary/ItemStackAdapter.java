package io.github.totom3.commons.binary;

import java.io.DataInputStream;
import java.io.IOException;
import net.minecraft.server.v1_8_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Totom3
 */
public class ItemStackAdapter implements BinaryAdapter<ItemStack> {

    
    
    @Override
    public ItemStack read(DeserializationContext context) throws IOException {
	if (context.readBoolean()) {
	    return null;
	}

	NBTTagCompound nbt = NBTCompressedStreamTools.a((DataInputStream) context.in());
	net.minecraft.server.v1_8_R3.ItemStack nms = net.minecraft.server.v1_8_R3.ItemStack.createStack(nbt);
	return CraftItemStack.asCraftMirror(nms);
    }

    @Override
    public void write(ItemStack item, SerializationContext context) throws IOException {
	net.minecraft.server.v1_8_R3.ItemStack stack = CraftItemStack.asNMSCopy(item);

	if (context.writeAndReturnBool(stack == null)) {
	    return;
	}
	
	NBTTagCompound nbt = stack.save(new NBTTagCompound());
	NBTCompressedStreamTools.a(nbt, context.out());
    }

}
