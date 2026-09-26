package com.apexmc.skyblock.paper.gui;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/**
 * Hard-locks every plugin menu: clicks, shift clicks, number key swaps, drags,
 * double-click collection and offhand swaps are all cancelled before any menu
 * logic runs, so decoration items can never be extracted.
 */
public final class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView() == null ? event.getInventory() : event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof Menu menu)) {
            return;
        }
        event.setCancelled(true);
        HumanEntity clicker = event.getWhoClicked();
        if (!(clicker instanceof Player player)) {
            return;
        }
        ClickType click = event.getClick();
        if (click == ClickType.DOUBLE_CLICK || click == ClickType.CREATIVE
                || click == ClickType.SWAP_OFFHAND || click == ClickType.NUMBER_KEY
                || event.getAction() == InventoryAction.COLLECT_TO_CURSOR
                || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD
                || event.getAction() == InventoryAction.HOTBAR_SWAP) {
            return;
        }
        // Only clicks inside the menu itself are dispatched.
        if (event.getClickedInventory() != top) {
            return;
        }
        int slot = event.getSlot();
        if (slot < 0 || slot >= menu.size()) {
            return;
        }
        menu.onClick(player, slot, click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView() == null ? event.getInventory() : event.getView().getTopInventory();
        if (top != null && top.getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }
}
