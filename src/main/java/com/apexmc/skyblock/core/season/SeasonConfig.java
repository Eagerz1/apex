package com.apexmc.skyblock.core.season;

import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.core.config.ConfigException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Seasonal configuration.
 *
 * @param seasonId        current season identifier (data is tagged with it)
 * @param displayName     GUI name
 * @param xpMultiplier    multiplier applied to every XP gain this season
 * @param tokenMultiplier multiplier applied to token gains
 * @param resetTargets    which data is wiped on a season reset
 * @param protectedStats  statistic keys that always survive a reset
 * @param keepCompanions  whether companions survive a reset (purchased content)
 * @param keepTokens      whether Sky Token balances survive a reset
 */
public record SeasonConfig(String seasonId,
                           String displayName,
                           double xpMultiplier,
                           double tokenMultiplier,
                           Set<String> resetTargets,
                           Set<String> protectedStats,
                           boolean keepCompanions,
                           boolean keepTokens) {

    /** Every reset target the plugin understands. */
    public static final List<String> VALID_TARGETS =
            List.of("island_xp", "mastery_xp", "role_xp", "omnitool_upgrades", "generators", "spawners", "stats", "unlocks");

    public SeasonConfig {
        resetTargets = Set.copyOf(resetTargets);
        protectedStats = Set.copyOf(protectedStats);
    }

    public static SeasonConfig fromConfig(Cfg cfg) {
        String seasonId = cfg.getString("season-id", "season-1").trim().toLowerCase(Locale.ROOT);
        if (seasonId.isEmpty()) {
            throw new ConfigException(cfg.path() + ".season-id", "must not be empty");
        }
        double xpMultiplier = cfg.getDouble("xp-multiplier", 1.0d);
        double tokenMultiplier = cfg.getDouble("token-multiplier", 1.0d);
        if (xpMultiplier < 0 || tokenMultiplier < 0) {
            throw new ConfigException(cfg.path(), "season multipliers must not be negative");
        }
        Set<String> targets = new java.util.LinkedHashSet<>();
        for (String target : cfg.getStringList("reset")) {
            String normalised = target.trim().toLowerCase(Locale.ROOT).replace('-', '_');
            if (!VALID_TARGETS.contains(normalised)) {
                throw new ConfigException(cfg.path() + ".reset",
                        "unknown reset target '" + target + "' (valid: " + String.join(", ", VALID_TARGETS) + ")");
            }
            targets.add(normalised);
        }
        Set<String> protectedStats = new java.util.LinkedHashSet<>(cfg.getStringList("protected-stats"));
        return new SeasonConfig(
                seasonId,
                cfg.getString("display-name", seasonId),
                xpMultiplier,
                tokenMultiplier,
                targets,
                protectedStats,
                cfg.getBoolean("keep-companions", true),
                cfg.getBoolean("keep-tokens", false));
    }

    public boolean resets(String target) {
        return resetTargets.contains(target);
    }
}
