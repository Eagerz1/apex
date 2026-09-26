package com.apexmc.skyblock.core.companions;

import java.util.List;

/**
 * A companion type.
 *
 * @param id              companion identifier
 * @param displayName     GUI name
 * @param icon            GUI material key
 * @param ability         ability identifier (e.g. {@code mining_xp_bonus})
 * @param abilityBase     ability magnitude at level 1
 * @param abilityPerLevel ability magnitude gained per level
 * @param baseRarityId    rarity the companion is obtained at
 * @param description     GUI lore
 * @param requiredIslandLevel island level required to obtain/use the companion
 */
public record CompanionDefinition(String id,
                                  String displayName,
                                  String icon,
                                  String ability,
                                  double abilityBase,
                                  double abilityPerLevel,
                                  String baseRarityId,
                                  List<String> description,
                                  int requiredIslandLevel) {

    public CompanionDefinition {
        description = List.copyOf(description);
    }
}
