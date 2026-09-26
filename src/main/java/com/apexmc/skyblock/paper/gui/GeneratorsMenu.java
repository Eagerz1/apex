package com.apexmc.skyblock.paper.gui;

import com.apexmc.skyblock.core.data.GeneratorState;
import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.generators.GeneratorTier;
import com.apexmc.skyblock.core.generators.GeneratorType;
import com.apexmc.skyblock.core.generators.UpgradeOutcome;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import com.apexmc.skyblock.paper.util.Items;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Lists the island's generators and allows tier upgrades. */
public final class GeneratorsMenu extends Menu {

    private final Menus menus;

    public GeneratorsMenu(SkyblockRuntime runtime, Menus menus) {
        super(runtime, menus.title("generators-title", "&8Generators"), 6);
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
        List<GeneratorState> states = new ArrayList<>(runtime.services().generators().ofIsland(island.islandId()));
        states.sort(java.util.Comparator.comparing(state -> state.position().key()));

        int slot = 0;
        for (GeneratorState state : states) {
            if (slot >= 45) {
                break;
            }
            GeneratorType type = runtime.services().generators().type(state.typeId()).orElse(null);
            if (type == null) {
                continue;
            }
            GeneratorTier tier = type.tier(state.tier()).orElse(null);
            UpgradeOutcome preview = runtime.services().generators().previewUpgrade(island, profile, state);
            List<String> lore = new ArrayList<>();
            lore.add("&7Location: &f" + state.position().world() + " "
                    + state.position().x() + ", " + state.position().y() + ", " + state.position().z());
            lore.add("&7Tier: &e" + state.tier() + "&7/&e" + type.maxTier());
            if (tier != null) {
                lore.add("&7Speed: &e" + (tier.intervalTicks() / 20.0d) + "s");
                lore.add("&7Output: &e" + tier.outputAmount() + " per cycle");
            }
            if (preview.success()) {
                lore.add("&7Upgrade cost: &e" + preview.tokensSpent() + " "
                        + runtime.services().tokens().displayNamePlural());
                lore.add("&aClick to upgrade");
            } else {
                lore.add("&c" + SkyblockRuntime.prettify(preview.reason()));
            }
            set(slot, Items.build(type.icon(), Material.COBBLESTONE, "&f" + type.displayName(), lore),
                    (clicker, right) -> {
                        UpgradeOutcome outcome = runtime.services().generators().upgrade(island, profile, state);
                        if (outcome.success()) {
                            runtime.send(clicker, "generator-upgraded", "tier", outcome.newTier());
                        } else if ("max-tier".equals(outcome.reason())) {
                            runtime.send(clicker, "generator-max-tier");
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
            set(22, Items.build("COBBLESTONE", Material.COBBLESTONE, "&7No generators placed yet",
                    List.of("&8Place a generator block on your island")));
        }
        backButton(49, menus.progression());
    }
}
