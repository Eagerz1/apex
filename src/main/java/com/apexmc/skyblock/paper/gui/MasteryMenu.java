package com.apexmc.skyblock.paper.gui;

import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.mastery.MasteryCategory;
import com.apexmc.skyblock.core.mastery.MasteryUnlock;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import com.apexmc.skyblock.paper.util.Items;
import com.apexmc.skyblock.paper.util.Text;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Mastery categories with their unlock tracks. */
public final class MasteryMenu extends Menu {

    private final Menus menus;

    public MasteryMenu(SkyblockRuntime runtime, Menus menus) {
        super(runtime, menus.title("mastery-title", "&8Island Mastery"), 5);
        this.menus = menus;
    }

    @Override
    protected void build(Player player) {
        fill(menus.filler());
        IslandProfile island = runtime.island(player).orElse(null);
        int slot = 10;
        for (MasteryCategory category : runtime.services().mastery().categories()) {
            int level = island == null ? 0 : runtime.services().mastery().levelOf(island, category.id());
            double progress = island == null ? 0.0d
                    : runtime.services().mastery().track(island, category.id()).progress();
            List<String> lore = new ArrayList<>();
            lore.add("&7Level: &e" + level + "&7/&e" + category.maxLevel());
            lore.add(Text.bar(progress, 20, "&d", "&8"));
            lore.add("&7Unlocks:");
            for (MasteryUnlock unlock : category.unlocks()) {
                boolean owned = level >= unlock.level();
                lore.add((owned ? "&a \u2714 " : "&8 \u2716 ") + unlock.label() + " &7(lvl " + unlock.level() + ")");
            }
            set(slot, Items.build(category.icon(), Material.BOOK, "&d" + category.displayName(), lore));
            slot += slot % 9 == 7 ? 4 : 1;
            if (slot >= size() - 9) {
                break;
            }
        }
        backButton(40, menus.progression());
    }
}
