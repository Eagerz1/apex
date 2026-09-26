package com.apexmc.skyblock.paper.gui;

import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.data.SpawnerState;
import com.apexmc.skyblock.core.generators.UpgradeOutcome;
import com.apexmc.skyblock.core.spawners.SpawnerTier;
import com.apexmc.skyblock.core.spawners.SpawnerType;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import com.apexmc.skyblock.paper.util.Items;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Lists the island's spawners with stack sizes and tier upgrades. */
public final class SpawnersMenu extends Menu {

    private final Menus menus;

    public SpawnersMenu(SkyblockRuntime runtime, Menus menus) {
        super(runtime, menus.title("spawners-title", "&8Spawners"), 6);
        this.menus = menus;
    }

    @Override
    protected void build(Player player) {
        fill(menus.filler());
        PlayerProfile profile = runtime.profile(player).orElse(null);
        IslandProfile island = profile == null ? null : runtime.island(profile).orElse(null);
        if (profile == null || island == null) {
            set(22, Items.build("BARRIER", Material.BARRIER, "&cNo island loaded", List.of()));
            return;
        }
        List<SpawnerState> states = new ArrayList<>(runtime.services().spawners().ofIsland(island.islandId()));
        states.sort(java.util.Comparator.comparing(state -> state.position().key()));

        int slot = 0;
        for (SpawnerState state : states) {
            if (slot >= 45) {
                break;
            }
            SpawnerType type = runtime.services().spawners().type(state.typeId()).orElse(null);
            if (type == null) {
                continue;
            }
            SpawnerTier tier = type.tier(state.tier()).orElse(null);
            UpgradeOutcome preview = runtime.services().spawners().previewUpgrade(island, profile, state);
            List<String> lore = new ArrayList<>();
            lore.add("&7Location: &f" + state.position().world() + " "
                    + state.position().x() + ", " + state.position().y() + ", " + state.position().z());
            lore.add("&7Tier: &e" + state.tier() + "&7/&e" + type.maxTier());
            lore.add("&7Stack: &e" + state.stackSize() + "&7/&e" + type.maxStackSize());
            if (tier != null) {
                lore.add("&7Mob: &e" + SkyblockRuntime.prettify(tier.entityType()));
                lore.add("&7Interval: &e" + (tier.intervalTicks() / 20.0d) + "s");
            }
            if (preview.success()) {
                lore.add("&7Upgrade cost: &e" + preview.tokensSpent() + " "
                        + runtime.services().tokens().displayNamePlural());
                lore.add("&aClick to upgrade");
            } else {
                lore.add("&c" + SkyblockRuntime.prettify(preview.reason()));
            }
            set(slot, Items.build(type.icon(), Material.SPAWNER, "&5" + type.displayName(), lore),
                    (clicker, right) -> {
                        UpgradeOutcome outcome = runtime.services().spawners().upgrade(island, profile, state);
                        if (outcome.success()) {
                            runtime.send(clicker, "spawner-upgraded", "tier", outcome.newTier());
                        } else if ("insufficient-tokens".equals(outcome.reason())) {
                            runtime.send(clicker, "tokens-insufficient", "amount", preview.tokensSpent(),
                                    "currency", runtime.services().tokens().displayNamePlural());
                        } else {
                            runtime.send(clicker, "gui-locked");
                        }
                        refresh(clicker);
                    });
            slot++;
        }
        if (states.isEmpty()) {
            set(22, Items.build("SPAWNER", Material.SPAWNER, "&7No spawners placed yet",
                    List.of("&8Unlocked at island level 7")));
        }
        backButton(49, menus.progression());
    }
}
