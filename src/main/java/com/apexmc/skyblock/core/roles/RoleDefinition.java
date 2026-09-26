package com.apexmc.skyblock.core.roles;

import com.apexmc.skyblock.core.progression.XpCurve;
import java.util.List;
import java.util.Map;

/**
 * Configured role (Miner, Farmer, Fisher, Slayer, Logger, Universal, ...).
 *
 * @param id             normalised identifier
 * @param displayName    GUI name
 * @param icon           GUI material key (resolved by the Bukkit layer)
 * @param description    GUI lore
 * @param curve          XP curve for the role level
 * @param omniToolId     identifier of the OmniTool bound to this role
 * @param perksByLevel   perks granted at each level
 * @param requiredIslandLevel island level required before the role can be selected
 */
public record RoleDefinition(String id,
                             String displayName,
                             String icon,
                             List<String> description,
                             XpCurve curve,
                             String omniToolId,
                             Map<Integer, List<RolePerk>> perksByLevel,
                             int requiredIslandLevel) {

    public RoleDefinition {
        description = List.copyOf(description);
        perksByLevel = Map.copyOf(perksByLevel);
    }

    public int maxLevel() {
        return curve.maxLevel();
    }

    /** All perks that are active at (or below) the supplied level. */
    public List<RolePerk> perksUpTo(int level) {
        List<RolePerk> perks = new java.util.ArrayList<>();
        for (Map.Entry<Integer, List<RolePerk>> entry : perksByLevel.entrySet()) {
            if (entry.getKey() <= level) {
                perks.addAll(entry.getValue());
            }
        }
        return perks;
    }

    /** Sums the value of all active perks of a type (e.g. total xp bonus). */
    public double perkValue(int level, String perkType) {
        double total = 0.0d;
        for (RolePerk perk : perksUpTo(level)) {
            if (perk.type().equalsIgnoreCase(perkType)) {
                total += perk.value();
            }
        }
        return total;
    }
}
