package com.apexmc.skyblock.paper.util;

import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Small builder for GUI items; unknown materials never crash a menu. */
public final class Items {

    private Items() {
    }

    public static Material material(String key, Material fallback) {
        if (key == null || key.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(key.trim().toUpperCase(java.util.Locale.ROOT));
        return material == null ? fallback : material;
    }

    public static ItemStack build(String materialKey, Material fallback, String name, List<String> lore) {
        ItemStack item = new ItemStack(material(materialKey, fallback));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(Text.colour(name));
            }
            if (lore != null) {
                meta.setLore(Text.colour(lore));
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack filler(String materialKey) {
        return build(materialKey, Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
    }
}
