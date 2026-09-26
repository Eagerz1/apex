package com.apexmc.skyblock.core.season;

/**
 * Summary of what a season reset actually changed, used for logging and admin
 * feedback.
 */
public record SeasonResetReport(String fromSeason,
                                String toSeason,
                                boolean islandXpReset,
                                boolean masteryReset,
                                boolean roleXpReset,
                                boolean toolUpgradesReset,
                                boolean statsReset,
                                boolean unlocksReset,
                                boolean tokensReset,
                                boolean companionsReset) {

    public boolean changedAnything() {
        return islandXpReset || masteryReset || roleXpReset || toolUpgradesReset
                || statsReset || unlocksReset || tokensReset || companionsReset;
    }
}
