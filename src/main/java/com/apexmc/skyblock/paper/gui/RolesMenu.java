package com.apexmc.skyblock.paper.gui;

import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.roles.RoleDefinition;
import com.apexmc.skyblock.core.roles.RolePerk;
import com.apexmc.skyblock.core.roles.RoleService;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import com.apexmc.skyblock.paper.util.Items;
import com.apexmc.skyblock.paper.util.Text;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Role selection. Switching never deletes progression of the previous role. */
public final class RolesMenu extends Menu {

    private final Menus menus;

    public RolesMenu(SkyblockRuntime runtime, Menus menus) {
        super(runtime, menus.title("roles-title", "&8Roles"), 5);
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
        int islandLevel = runtime.island(profile)
                .map(island -> runtime.services().islands().levelOf(island)).orElse(0);
        String activeId = runtime.services().roles().activeRoleId(profile);

        int slot = 10;
        for (RoleDefinition role : runtime.services().roles().registry().all()) {
            int level = runtime.services().roles().levelOf(profile, role.id());
            double progress = runtime.services().roles().track(profile, role.id()).progress();
            boolean active = role.id().equals(activeId);
            boolean unlocked = islandLevel >= role.requiredIslandLevel();

            List<String> lore = new ArrayList<>(role.description());
            lore.add("&7Level: &e" + level + "&7/&e" + role.maxLevel());
            lore.add(Text.bar(progress, 20, "&a", "&8"));
            lore.add("&7Perks:");
            for (RolePerk perk : role.perksUpTo(role.maxLevel())) {
                lore.add("&8 - " + perk.label());
            }
            if (!unlocked) {
                lore.add("&cRequires island level " + role.requiredIslandLevel());
            } else if (active) {
                lore.add("&aActive role");
            } else {
                lore.add("&eClick to select &7(progress is kept)");
            }

            set(slot, Items.build(role.icon(), Material.DIAMOND_PICKAXE,
                            (active ? "&a" : "&f") + role.displayName(), lore),
                    (clicker, right) -> {
                        RoleService.RoleChange change = runtime.services().roles()
                                .selectRole(profile, role.id(), islandLevel);
                        if (change.success()) {
                            runtime.send(clicker, "role-selected", "role", role.displayName());
                        } else {
                            switch (change.reason()) {
                                case "already-active" -> runtime.send(clicker, "role-already-active");
                                case "island-level-required" -> runtime.send(clicker, "role-island-level",
                                        "level", role.requiredIslandLevel());
                                default -> runtime.send(clicker, "role-unknown");
                            }
                        }
                        refresh(clicker);
                    });
            slot += slot % 9 == 7 ? 4 : 1;
        }
        backButton(40, menus.progression());
    }
}
