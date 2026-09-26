package com.apexmc.skyblock.paper.gui;

import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.omnitool.OmniToolDefinition;
import com.apexmc.skyblock.core.omnitool.OmniToolService;
import com.apexmc.skyblock.core.omnitool.UpgradeNode;
import com.apexmc.skyblock.core.omnitool.UpgradeResult;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import com.apexmc.skyblock.paper.util.Items;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** OmniTool upgrade tree for one role. */
public final class OmniToolMenu extends Menu {

    private final Menus menus;
    private final String toolId;

    public OmniToolMenu(SkyblockRuntime runtime, Menus menus, String toolId) {
        super(runtime, menus.title("omnitool-title", "&8OmniTool Upgrades"), 5);
        this.menus = menus;
        this.toolId = toolId;
    }

    @Override
    protected void build(Player player) {
        fill(menus.filler());
        PlayerProfile profile = runtime.profile(player).orElse(null);
        OmniToolDefinition tool = runtime.services().omniTools().tool(toolId).orElse(null);
        if (profile == null || tool == null) {
            set(22, Items.build("BARRIER", Material.BARRIER, "&cOmniTool unavailable", List.of()));
            return;
        }
        IslandProfile island = runtime.island(profile).orElse(null);
        int islandLevel = island == null ? 0 : runtime.services().islands().levelOf(island);
        int roleLevel = runtime.services().roles().levelOf(profile, tool.roleId());

        OmniToolService.UpgradeContext context = new OmniToolService.UpgradeContext(roleLevel, islandLevel,
                unlock -> island != null && runtime.services().islands().isUnlocked(island, unlock));

        int slot = 10;
        for (UpgradeNode node : tool.nodes().values()) {
            int level = runtime.services().omniTools().nodeLevel(profile, tool.id(), node.id());
            UpgradeResult preview = runtime.services().omniTools().preview(profile, tool.id(), node.id(), context);

            List<String> lore = new ArrayList<>(node.description());
            lore.add("&7Level: &e" + level + "&7/&e" + node.maxLevel());
            lore.add("&7Current effect: &e" + String.format(java.util.Locale.ROOT, "%.2f", node.valueAt(level)));
            if (level < node.maxLevel()) {
                lore.add("&7Cost: &e" + node.tokenCost(level) + " "
                        + runtime.services().tokens().displayNamePlural());
                if (node.moneyCost(level) > 0) {
                    lore.add("&7Money: &e" + runtime.services().economy().format(node.moneyCost(level)));
                }
                lore.add("&7Requires role level &e" + node.requiredRoleLevelFor(level));
                if (node.requiredIslandLevel() > 0) {
                    lore.add("&7Requires island level &e" + node.requiredIslandLevel());
                }
            }
            lore.add(preview.success() ? "&aClick to upgrade" : "&c" + SkyblockRuntime.prettify(preview.reason()));

            set(slot, Items.build(node.icon(), Material.PAPER, "&b" + node.displayName(), lore),
                    (clicker, right) -> {
                        UpgradeResult result = runtime.services().omniTools()
                                .upgrade(profile, tool.id(), node.id(), context);
                        if (result.success()) {
                            runtime.send(clicker, "upgrade-success", "node", node.displayName(), "level", result.newLevel());
                            clicker.playSound(clicker.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.4f);
                            if (result.tokensSpent() > 0) {
                                runtime.fireTokenChange(profile.playerId(), -result.tokensSpent(),
                                        profile.tokens(), "omnitool-upgrade");
                            }
                        } else {
                            switch (result.reason()) {
                                case "max-level" -> runtime.send(clicker, "upgrade-max-level");
                                case "role-level" -> runtime.send(clicker, "upgrade-role-level",
                                        "level", node.requiredRoleLevelFor(result.newLevel()));
                                case "island-level" -> runtime.send(clicker, "upgrade-island-level",
                                        "level", node.requiredIslandLevel());
                                case "locked" -> runtime.send(clicker, "upgrade-locked");
                                case "economy-unavailable" -> runtime.send(clicker, "economy-unavailable");
                                case "insufficient-tokens" -> runtime.send(clicker, "tokens-insufficient",
                                        "amount", node.tokenCost(result.newLevel()) - profile.tokens(),
                                        "currency", runtime.services().tokens().displayNamePlural());
                                default -> runtime.send(clicker, "gui-locked");
                            }
                        }
                        refresh(clicker);
                    });
            slot += slot % 9 == 7 ? 4 : 1;
            if (slot >= size() - 9) {
                break;
            }
        }
        backButton(40, menus.progression());
    }
}
