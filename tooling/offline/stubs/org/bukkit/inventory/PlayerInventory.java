package org.bukkit.inventory;

public interface PlayerInventory extends Inventory {
    ItemStack getItemInMainHand();
    void setItemInMainHand(ItemStack item);
    boolean contains(org.bukkit.Material material);
    int first(org.bukkit.Material material);
}
