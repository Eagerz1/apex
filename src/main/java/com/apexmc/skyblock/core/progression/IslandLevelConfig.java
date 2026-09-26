package com.apexmc.skyblock.core.progression;

import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.core.config.ConfigException;
import com.apexmc.skyblock.core.util.Ids;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Parsed and validated island level table plus its derived XP curve. */
public final class IslandLevelConfig {

    private final List<IslandLevel> levels;
    private final XpCurve curve;
    private final Map<String, Integer> unlockLevels;
    private final Map<String, Double> xpSources;

    private IslandLevelConfig(List<IslandLevel> levels, Map<String, Double> xpSources) {
        this.levels = List.copyOf(levels);
        this.xpSources = Map.copyOf(xpSources);
        List<Long> requirements = new ArrayList<>(levels.size());
        Map<String, Integer> unlocks = new LinkedHashMap<>();
        for (IslandLevel level : levels) {
            requirements.add(level.xpRequired());
            for (String unlock : level.unlocks()) {
                unlocks.putIfAbsent(unlock, level.level());
            }
        }
        this.curve = XpCurve.table(requirements);
        this.unlockLevels = Map.copyOf(unlocks);
    }

    public static IslandLevelConfig fromConfig(Cfg cfg) {
        Cfg levelsSection = cfg.section("levels");
        Map<Integer, Cfg> ordered = new java.util.TreeMap<>();
        for (Map.Entry<String, Cfg> entry : levelsSection.childSections().entrySet()) {
            int level;
            try {
                level = Integer.parseInt(entry.getKey().trim());
            } catch (NumberFormatException ex) {
                throw new ConfigException(levelsSection.path() + "." + entry.getKey(), "level keys must be whole numbers");
            }
            if (level < 1) {
                throw new ConfigException(levelsSection.path() + "." + entry.getKey(), "level numbers start at 1");
            }
            ordered.put(level, entry.getValue());
        }
        if (ordered.isEmpty()) {
            throw new ConfigException(levelsSection.path(), "at least one island level must be configured");
        }

        List<IslandLevel> levels = new ArrayList<>();
        int expected = 1;
        for (Map.Entry<Integer, Cfg> entry : ordered.entrySet()) {
            if (entry.getKey() != expected) {
                throw new ConfigException(levelsSection.path(), "island levels must be contiguous; expected level "
                        + expected + " but found " + entry.getKey());
            }
            Cfg section = entry.getValue();
            long xp = section.getLong("xp-required");
            if (xp <= 0) {
                throw new ConfigException(section.path() + ".xp-required", "must be greater than zero");
            }
            List<String> unlocks = new ArrayList<>();
            for (String raw : section.getStringList("unlocks")) {
                unlocks.add(Ids.normalize(raw));
            }
            levels.add(new IslandLevel(
                    expected,
                    xp,
                    section.getString("title", "Island Level " + expected),
                    unlocks,
                    Math.max(0L, section.getLong("token-reward", 0L)),
                    Math.max(0.0d, section.getDouble("money-reward", 0.0d)),
                    section.getStringList("commands"),
                    section.getStringList("description")));
            expected++;
        }

        Map<String, Double> sources = new LinkedHashMap<>();
        Cfg sourcesSection = cfg.sectionOrEmpty("xp-sources");
        for (String key : sourcesSection.keys()) {
            double value = sourcesSection.getDouble(key);
            if (value < 0) {
                throw new ConfigException(sourcesSection.path() + "." + key, "xp source values must not be negative");
            }
            sources.put(key.toLowerCase(Locale.ROOT), value);
        }
        return new IslandLevelConfig(levels, sources);
    }

    public List<IslandLevel> levels() {
        return levels;
    }

    public XpCurve curve() {
        return curve;
    }

    public int maxLevel() {
        return curve.maxLevel();
    }

    public Optional<IslandLevel> level(int level) {
        if (level < 1 || level > levels.size()) {
            return Optional.empty();
        }
        return Optional.of(levels.get(level - 1));
    }

    /** Island level at which an unlock becomes available, if it is configured at all. */
    public Optional<Integer> unlockLevel(String unlockId) {
        return Optional.ofNullable(unlockLevels.get(Ids.normalize(unlockId)));
    }

    public Map<String, Integer> unlockLevels() {
        return unlockLevels;
    }

    /** Island XP granted per unit of a gameplay source (e.g. {@code block_mined}). */
    public double xpPerSource(String sourceId) {
        return xpSources.getOrDefault(sourceId.toLowerCase(Locale.ROOT), 0.0d);
    }

    public Map<String, Double> xpSources() {
        return xpSources;
    }
}
