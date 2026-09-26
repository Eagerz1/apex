package com.apexmc.skyblock.paper.gui;

import com.apexmc.skyblock.paper.SkyblockRuntime;
import com.apexmc.skyblock.paper.util.Items;
import com.apexmc.skyblock.paper.util.Text;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Base class for every menu. Menus are their own {@link InventoryHolder}, which
 * is how the click listener recognises them - titles are never trusted.
 *
 * <p>All inventory interaction is cancelled by {@code MenuListener}; menus only
 * receive {@link #onClick(Player, int, boolean)} for slots that registered a
 * button, so shift-clicks, number keys, drags and double clicks can never move
 * decoration items.</p>
 */
public abstract class Menu implements InventoryHolder {

    protected final SkyblockRuntime runtime;
    private final Map<Integer, Button> buttons = new HashMap<>();
    private Inventory inventory;
    private final String title;
    private final int size;

    protected Menu(SkyblockRuntime runtime, String title, int rows) {
        this.runtime = runtime;
        this.title = Text.colour(title);
        this.size = Math.max(9, Math.min(54, rows * 9));
    }

    /** A clickable slot. */
    public interface Button {
        void click(Player player, boolean rightClick);
    }

    protected abstract void build(Player player);

    public void open(Player player) {
        buttons.clear();
        inventory = Bukkit.createInventory(this, size, title);
        build(player);
        player.openInventory(inventory);
    }

    /** Rebuilds the contents in place (used after an upgrade or role change). */
    public void refresh(Player player) {
        if (inventory == null) {
            open(player);
            return;
        }
        buttons.clear();
        inventory.clear();
        build(player);
        player.updateInventory();
    }

    protected void set(int slot, ItemStack item) {
        if (inventory != null && slot >= 0 && slot < size) {
            inventory.setItem(slot, item);
        }
    }

    protected void set(int slot, ItemStack item, Button button) {
        set(slot, item);
        if (button != null) {
            buttons.put(slot, button);
        }
    }

    protected void fill(String materialKey) {
        ItemStack filler = Items.filler(materialKey);
        for (int slot = 0; slot < size; slot++) {
            set(slot, filler);
        }
    }

    protected void backButton(int slot, Menu parent) {
        set(slot, Items.build("ARROW", Material.ARROW, "&7Back", List.of()),
                (player, right) -> parent.open(player));
    }

    /** Dispatches a click to the registered button; unknown slots do nothing. */
    public void onClick(Player player, int slot, boolean rightClick) {
        Button button = buttons.get(slot);
        if (button != null) {
            button.click(player, rightClick);
        }
    }

    public int size() {
        return size;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
