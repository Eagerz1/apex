package org.bukkit.inventory;

import org.bukkit.Material;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemStack implements Cloneable {
    public ItemStack(Material type) { }
    public ItemStack(Material type, int amount) { }
    public Material getType() { return Material.AIR; }
    public void setType(Material type) { }
    public int getAmount() { return 1; }
    public void setAmount(int amount) { }
    public ItemMeta getItemMeta() { return null; }
    public boolean setItemMeta(ItemMeta meta) { return true; }
    public boolean hasItemMeta() { return false; }
    @Override public ItemStack clone() { return this; }
}
