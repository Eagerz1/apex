package org.bukkit.entity;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

public interface HumanEntity extends LivingEntity {
    InventoryView getOpenInventory();
    void closeInventory();
    Inventory getInventory();
}
