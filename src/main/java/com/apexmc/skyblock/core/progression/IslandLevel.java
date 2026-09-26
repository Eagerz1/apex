package com.apexmc.skyblock.core.progression;

import java.util.List;

/**
 * A single configured island level.
 *
 * @param level        level number (1-based, contiguous)
 * @param xpRequired   XP required to advance from the previous level to this one
 * @param title        display title shown in GUIs
 * @param unlocks      unlock identifiers granted at this level (mechanics, not numbers)
 * @param tokenReward  Sky Tokens paid out on reaching the level
 * @param moneyReward  Vault money paid out on reaching the level (ignored without Vault)
 * @param commands     console commands executed on reaching the level
 * @param description  GUI description lines
 */
public record IslandLevel(int level,
                          long xpRequired,
                          String title,
                          List<String> unlocks,
                          long tokenReward,
                          double moneyReward,
                          List<String> commands,
                          List<String> description) {

    public IslandLevel {
        unlocks = List.copyOf(unlocks);
        commands = List.copyOf(commands);
        description = List.copyOf(description);
    }
}
