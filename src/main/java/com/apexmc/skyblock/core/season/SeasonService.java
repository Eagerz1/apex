package com.apexmc.skyblock.core.season;

import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Seasonal progression. Profiles are tagged with the season they belong to; when
 * a profile from a previous season is loaded it is migrated exactly once,
 * applying only the configured reset targets. Purchased/protected content
 * (companions, optionally tokens, protected statistics) is never wiped.
 */
public final class SeasonService {

    private SeasonConfig config;

    public SeasonService(SeasonConfig config) {
        this.config = config;
    }

    public void reload(SeasonConfig config) {
        this.config = config;
    }

    public SeasonConfig config() {
        return config;
    }

    public String currentSeasonId() {
        return config.seasonId();
    }

    public boolean needsReset(PlayerProfile profile) {
        return !config.seasonId().equalsIgnoreCase(profile.seasonId());
    }

    public boolean needsReset(IslandProfile island) {
        return !config.seasonId().equalsIgnoreCase(island.seasonId());
    }

    /** Applies the configured reset to a player profile (idempotent per season). */
    public SeasonResetReport applyTo(PlayerProfile profile) {
        String from = profile.seasonId();
        if (!needsReset(profile)) {
            return new SeasonResetReport(from, config.seasonId(), false, false, false, false, false, false, false, false);
        }
        boolean roleXpReset = false;
        boolean toolReset = false;
        boolean statsReset = false;
        boolean tokensReset = false;
        boolean companionsReset = false;

        if (config.resets("role_xp")) {
            List<String> roles = new ArrayList<>(profile.roleXpView().keySet());
            for (String role : roles) {
                profile.roleXp(role, 0L);
            }
            roleXpReset = true;
        }
        if (config.resets("omnitool_upgrades")) {
            for (Map.Entry<String, Map<String, Integer>> entry : profile.toolUpgradesView().entrySet()) {
                profile.toolUpgrades(entry.getKey()).clear();
            }
            toolReset = true;
        }
        if (config.resets("stats")) {
            List<String> keys = new ArrayList<>(profile.statsView().keySet());
            for (String key : keys) {
                if (!config.protectedStats().contains(key)) {
                    profile.stat(key, 0L);
                }
            }
            statsReset = true;
        }
        if (!config.keepTokens()) {
            profile.tokens(0L);
            tokensReset = true;
        }
        if (!config.keepCompanions()) {
            profile.companions().clear();
            profile.activeCompanionId(null);
            companionsReset = true;
        }
        profile.seasonId(config.seasonId());
        profile.markDirty();
        return new SeasonResetReport(from, config.seasonId(), false, false,
                roleXpReset, toolReset, statsReset, false, tokensReset, companionsReset);
    }

    /** Applies the configured reset to an island profile. */
    public SeasonResetReport applyTo(IslandProfile island) {
        String from = island.seasonId();
        if (!needsReset(island)) {
            return new SeasonResetReport(from, config.seasonId(), false, false, false, false, false, false, false, false);
        }
        boolean islandXpReset = false;
        boolean masteryReset = false;
        boolean unlocksReset = false;

        if (config.resets("island_xp")) {
            island.islandXp(0L);
            islandXpReset = true;
        }
        if (config.resets("mastery_xp")) {
            List<String> categories = new ArrayList<>(island.masteryXpView().keySet());
            for (String category : categories) {
                island.masteryXp(category, 0L);
            }
            masteryReset = true;
        }
        if (config.resets("unlocks")) {
            island.grantedUnlocks().clear();
            unlocksReset = true;
        }
        island.seasonId(config.seasonId());
        island.markDirty();
        return new SeasonResetReport(from, config.seasonId(), islandXpReset, masteryReset,
                false, false, false, unlocksReset, false, false);
    }

    /** Whether placed generators should be wiped when the season rolls over. */
    public boolean resetsGenerators() {
        return config.resets("generators");
    }

    public boolean resetsSpawners() {
        return config.resets("spawners");
    }

    public double xpMultiplier() {
        return config.xpMultiplier();
    }

    public double tokenMultiplier() {
        return config.tokenMultiplier();
    }
}
