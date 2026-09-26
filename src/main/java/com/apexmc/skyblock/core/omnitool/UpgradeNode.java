package com.apexmc.skyblock.core.omnitool;

import java.util.List;

/**
 * One configurable OmniTool upgrade node (efficiency, fortune, area mining,
 * auto replant, token find, ...).
 *
 * @param id                  node identifier
 * @param displayName         GUI name
 * @param icon                GUI material key
 * @param description         GUI lore
 * @param effect              effect identifier consumed by gameplay listeners
 * @param maxLevel            maximum node level
 * @param valuePerLevel       effect magnitude gained per level
 * @param baseValue           magnitude at level 0
 * @param tokenCostBase       token cost of the first level
 * @param tokenCostPerLevel   additional token cost per level
 * @param moneyCostBase       money cost of the first level (requires Vault)
 * @param moneyCostPerLevel   additional money cost per level
 * @param requiredRoleLevel   role level required for the first node level
 * @param roleLevelPerNodeLevel extra role levels required per node level
 * @param requiredIslandLevel island level required to touch this node at all
 * @param requiredUnlock      island unlock identifier required (empty = none)
 */
public record UpgradeNode(String id,
                          String displayName,
                          String icon,
                          List<String> description,
                          String effect,
                          int maxLevel,
                          double valuePerLevel,
                          double baseValue,
                          long tokenCostBase,
                          long tokenCostPerLevel,
                          double moneyCostBase,
                          double moneyCostPerLevel,
                          int requiredRoleLevel,
                          int roleLevelPerNodeLevel,
                          int requiredIslandLevel,
                          String requiredUnlock) {

    public UpgradeNode {
        description = List.copyOf(description);
    }

    /** Effect magnitude at a given node level. */
    public double valueAt(int level) {
        if (level <= 0) {
            return baseValue;
        }
        return baseValue + valuePerLevel * Math.min(level, maxLevel);
    }

    /** Token cost of advancing from {@code currentLevel} to {@code currentLevel + 1}. */
    public long tokenCost(int currentLevel) {
        if (currentLevel >= maxLevel) {
            return 0L;
        }
        return Math.max(0L, tokenCostBase + tokenCostPerLevel * currentLevel);
    }

    public double moneyCost(int currentLevel) {
        if (currentLevel >= maxLevel) {
            return 0.0d;
        }
        return Math.max(0.0d, moneyCostBase + moneyCostPerLevel * currentLevel);
    }

    /** Role level required to advance from {@code currentLevel} to the next level. */
    public int requiredRoleLevelFor(int currentLevel) {
        return requiredRoleLevel + roleLevelPerNodeLevel * currentLevel;
    }
}
