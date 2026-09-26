package com.apexmc.skyblock.paper.gui;

import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.progression.IslandLevel;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import com.apexmc.skyblock.paper.util.Items;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Paged view of the island level table, showing what each level unlocks. */
public final class IslandLevelsMenu extends Menu {

    private static final int PAGE_SIZE = 28;
    private static final int[] SLOTS = buildSlots();

    private final Menus menus;
    private int page;

    public IslandLevelsMenu(SkyblockRuntime runtime, Menus menus) {
        super(runtime, menus.title("island-title", "&8Island Levels"), 6);
        this.menus = menus;
    }

    private static int[] buildSlots() {
        int[] slots = new int[PAGE_SIZE];
        int index = 0;
        for (int row = 1; row <= 4; row++) {
            for (int column = 1; column <= 7; column++) {
                slots[index++] = row * 9 + column;
            }
        }
        return slots;
    }

    @Override
    protected void build(Player player) {
        fill(menus.filler());
        IslandProfile island = runtime.island(player).orElse(null);
        int current = island == null ? 0 : runtime.services().islands().levelOf(island);
        List<IslandLevel> levels = runtime.services().islands().config().levels();
        int pages = Math.max(1, (int) Math.ceil(levels.size() / (double) PAGE_SIZE));
        page = Math.max(0, Math.min(page, pages - 1));

        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = page * PAGE_SIZE + i;
            if (index >= levels.size()) {
                break;
            }
            IslandLevel level = levels.get(index);
            boolean reached = current >= level.level();
            List<String> lore = new ArrayList<>();
            lore.add(reached ? "&aReached" : "&7Locked");
            lore.add("&7XP required: &e" + level.xpRequired());
            if (level.tokenReward() > 0) {
                lore.add("&7Reward: &e" + level.tokenReward() + " " + runtime.services().tokens().displayNamePlural());
            }
            if (!level.unlocks().isEmpty()) {
                lore.add("&7Unlocks:");
                level.unlocks().forEach(unlock -> lore.add("&d \u2726 " + SkyblockRuntime.prettify(unlock)));
            }
            level.description().forEach(line -> lore.add("&8" + line));
            set(SLOTS[i], Items.build(reached ? "LIME_DYE" : "GRAY_DYE", Material.PAPER,
                    (reached ? "&a" : "&7") + "Level " + level.level() + " &8- &f" + level.title(), lore));
        }

        if (page > 0) {
            set(48, Items.build("ARROW", Material.ARROW, "&7Previous page", List.of()), (clicker, right) -> {
                page--;
                refresh(clicker);
            });
        }
        if ((page + 1) * PAGE_SIZE < levels.size()) {
            set(50, Items.build("ARROW", Material.ARROW, "&7Next page", List.of()), (clicker, right) -> {
                page++;
                refresh(clicker);
            });
        }
        backButton(49, menus.progression());
    }
}
