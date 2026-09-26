package com.apexmc.skyblock.paper.gui;

import com.apexmc.skyblock.core.companions.CompanionDefinition;
import com.apexmc.skyblock.core.companions.CompanionRarity;
import com.apexmc.skyblock.core.companions.CompanionService;
import com.apexmc.skyblock.core.data.CompanionInstance;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import com.apexmc.skyblock.paper.util.Items;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Companion collection: left click activates a companion, right click selects
 * it for merging (a second right click on a matching companion merges them).
 */
public final class CompanionsMenu extends Menu {

    private final Menus menus;
    private UUID mergeSelection;

    public CompanionsMenu(SkyblockRuntime runtime, Menus menus) {
        super(runtime, menus.title("companions-title", "&8Companions"), 6);
        this.menus = menus;
    }

    @Override
    protected void build(Player player) {
        fill(menus.filler());
        PlayerProfile profile = runtime.profile(player).orElse(null);
        if (profile == null) {
            set(22, Items.build("BARRIER", Material.BARRIER, "&cProfile still loading", List.of()));
            return;
        }
        CompanionService companions = runtime.services().companions();
        int slot = 0;
        for (CompanionInstance instance : new ArrayList<>(profile.companions())) {
            if (slot >= 45) {
                break;
            }
            CompanionDefinition definition = companions.definition(instance.definitionId()).orElse(null);
            CompanionRarity rarity = companions.rarity(instance.rarityId()).orElse(null);
            if (definition == null || rarity == null) {
                continue;
            }
            boolean active = profile.activeCompanionId().filter(instance.instanceId()::equals).isPresent();
            List<String> lore = new ArrayList<>(definition.description());
            lore.add("&7Rarity: " + rarity.colour() + rarity.displayName());
            lore.add("&7Level: &e" + companions.level(instance) + "&7/&e" + rarity.maxLevel());
            lore.add("&7Ability: &e" + SkyblockRuntime.prettify(definition.ability())
                    + " &7(" + String.format(java.util.Locale.ROOT, "%.3f", companions.abilityValue(instance)) + ")");
            lore.add(active ? "&aActive" : "&eLeft click to activate");
            if (!rarity.nextRarityId().isEmpty()) {
                lore.add("&7Right click to select for merging &8(" + rarity.mergeCost() + " tokens)");
            }
            if (instance.instanceId().equals(mergeSelection)) {
                lore.add("&dSelected for merging");
            }

            set(slot, Items.build(definition.icon(), Material.BONE,
                            rarity.colour() + definition.displayName(), lore),
                    (clicker, right) -> {
                        if (!right) {
                            companions.setActive(profile, instance.instanceId());
                            runtime.send(clicker, "companion-active", "companion", definition.displayName());
                            mergeSelection = null;
                        } else if (mergeSelection == null) {
                            mergeSelection = instance.instanceId();
                        } else if (mergeSelection.equals(instance.instanceId())) {
                            mergeSelection = null;
                        } else {
                            CompanionService.CompanionResult result = companions.merge(profile, mergeSelection,
                                    instance.instanceId(), runtime.services().config().companionMergeXpCarry());
                            if (result.success()) {
                                runtime.send(clicker, "companion-merged",
                                        "companion", definition.displayName(),
                                        "rarity", SkyblockRuntime.prettify(result.companion().rarityId()));
                            } else if ("insufficient-tokens".equals(result.reason())) {
                                runtime.send(clicker, "tokens-insufficient", "amount", rarity.mergeCost(),
                                        "currency", runtime.services().tokens().displayNamePlural());
                            } else {
                                runtime.send(clicker, "companion-merge-mismatch");
                            }
                            mergeSelection = null;
                        }
                        refresh(clicker);
                    });
            slot++;
        }
        if (profile.companions().isEmpty()) {
            set(22, Items.build("BONE", Material.BONE, "&7No companions yet",
                    List.of("&8Unlocked at island level 13")));
        }
        backButton(49, menus.progression());
    }
}
