package com.apexmc.skyblock.paper.gui;

import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.progression.LevelTrack;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import com.apexmc.skyblock.paper.util.Items;
import com.apexmc.skyblock.paper.util.Text;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Overview of every progression system with links to the detail menus. */
public final class ProgressionMenu extends Menu {

    private final Menus menus;

    public ProgressionMenu(SkyblockRuntime runtime, Menus menus) {
        super(runtime, menus.title("progression-title", "&8Progression Overview"), 5);
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
        IslandProfile island = runtime.island(profile).orElse(null);

        int islandLevel = island == null ? 0 : runtime.services().islands().levelOf(island);
        LevelTrack islandTrack = island == null ? null : runtime.services().islands().track(island);
        List<String> islandLore = new ArrayList<>();
        islandLore.add("&7Level: &e" + islandLevel + "&7/&e" + runtime.services().islands().config().maxLevel());
        if (islandTrack != null) {
            islandLore.add("&7Progress: &e" + Text.percent(islandTrack.progress()));
            islandLore.add(Text.bar(islandTrack.progress(), 20, "&a", "&8"));
            islandLore.add("&7XP to next: &e" + islandTrack.xpToNextLevel());
        }
        islandLore.add("&8Click to view the level track");
        set(10, Items.build("BEACON", Material.DIAMOND, "&bIsland Level", islandLore),
                (clicker, right) -> menus.islandLevels().open(clicker));

        String roleId = runtime.services().roles().activeRoleId(profile);
        var role = runtime.services().roles().registry().requireRole(roleId);
        LevelTrack roleTrack = runtime.services().roles().track(profile, roleId);
        set(12, Items.build(role.icon(), Material.DIAMOND_PICKAXE, "&eRole: &f" + role.displayName(), List.of(
                        "&7Level: &e" + roleTrack.level() + "&7/&e" + role.maxLevel(),
                        Text.bar(roleTrack.progress(), 20, "&a", "&8"),
                        "&8Click to change role")),
                (clicker, right) -> menus.roles().open(clicker));

        set(14, Items.build("BOOK", Material.BOOK, "&dIsland Mastery", List.of(
                        "&7Specialise your island",
                        "&8Click to open")),
                (clicker, right) -> menus.mastery().open(clicker));

        set(16, Items.build(role.icon(), Material.DIAMOND_PICKAXE, "&6OmniTool", List.of(
                        "&7Upgrade the tool of your active role",
                        "&8Click to open")),
                (clicker, right) -> menus.omniTool(role.omniToolId()).open(clicker));

        set(28, Items.build("COBBLESTONE", Material.COBBLESTONE, "&7Generators", List.of("&8Click to open")),
                (clicker, right) -> menus.generators().open(clicker));
        set(30, Items.build("SPAWNER", Material.SPAWNER, "&5Spawners", List.of("&8Click to open")),
                (clicker, right) -> menus.spawners().open(clicker));
        set(32, Items.build("BONE", Material.BONE, "&aCompanions", List.of("&8Click to open")),
                (clicker, right) -> menus.companions().open(clicker));

        String currency = runtime.services().tokens().displayNamePlural();
        set(34, Items.build("SUNFLOWER", Material.PAPER, "&e" + currency, List.of(
                "&7Balance: &e" + profile.tokens(),
                "&7Earned from mining, farming, fishing,",
                "&7slaying, logging and milestones")));
    }
}
