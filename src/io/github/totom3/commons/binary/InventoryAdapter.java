package io.github.totom3.commons.binary;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Totom3
 */
public class InventoryAdapter implements BinaryAdapter<Inventory> {

    @Override
    public Inventory read(DeserializationContext context) throws IOException {
	// Read title
	String title = context.readString();

	// Read size
	int size = context.readByte();

	// Read elements
	Map<Integer, ItemStack> map = context.readMap(Integer.class, ItemStack.class);
	ItemStack[] stacks = new ItemStack[size];
	for (Entry<Integer, ItemStack> entry : map.entrySet()) {
	    stacks[entry.getKey()] = entry.getValue();
	}

	Inventory inv = Bukkit.createInventory(null, size, title);
	inv.setContents(stacks);
	return inv;
    }

    @Override
    public void write(Inventory inv, SerializationContext context) throws IOException {
	// Write title
	context.writeString(inv.getTitle());

	ItemStack[] contents = inv.getContents();

	// Write size
	context.writeByte(contents.length);

	// Build items map
	Map<Integer, ItemStack> map = new HashMap<>();
	for (int i = 0; i < contents.length; ++i) {
	    ItemStack item = contents[i];
	    if (item != null) {
		map.put(i, item);
	    }
	}

	// Write items
	context.writeMap(map);
    }
}
