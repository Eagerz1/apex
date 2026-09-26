package com.apexmc.skyblock.core.companions;

import com.apexmc.skyblock.core.progression.XpCurve;

/**
 * A companion rarity. Defaults ship as Common 50, Rare 70, Epic 100, Mythic 200
 * maximum levels, all configurable.
 *
 * @param id             rarity identifier
 * @param displayName    GUI name
 * @param colour         colour code used in GUI names
 * @param maxLevel       maximum companion level at this rarity
 * @param curve          XP curve (its max level always matches {@code maxLevel})
 * @param abilityScale   multiplier applied to the companion ability magnitude
 * @param mergeCost      tokens required to merge two companions of this rarity
 * @param nextRarityId   rarity reached when merging ("" when this is the top rarity)
 */
public record CompanionRarity(String id,
                              String displayName,
                              String colour,
                              int maxLevel,
                              XpCurve curve,
                              double abilityScale,
                              long mergeCost,
                              String nextRarityId) {
}
