package org.bukkit.inventory;

import java.util.HashMap;

public interface Inventory extends Iterable<ItemStack> {
    int getSize();
    ItemStack getItem(int index);
    void setItem(int index, ItemStack item);
    HashMap<Integer, ItemStack> addItem(ItemStack... items);
    void clear();
    InventoryHolder getHolder();
    ItemStack[] getContents();
    @Override default java.util.Iterator<ItemStack> iterator() { return java.util.List.<ItemStack>of().iterator(); }
}
