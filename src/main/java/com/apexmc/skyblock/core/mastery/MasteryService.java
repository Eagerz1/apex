package com.apexmc.skyblock.core.mastery;

import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.core.config.ConfigException;
import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.progression.LevelTrack;
import com.apexmc.skyblock.core.progression.LevelUpResult;
import com.apexmc.skyblock.core.progression.XpCurve;
import com.apexmc.skyblock.core.util.Ids;
import com.apexmc.skyblock.core.util.Numbers;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Long term island mastery. Mastery XP is stored per island and is completely
 * separate from island XP, rewarding specialisation instead of raw grinding.
 */
public final class MasteryService {

    private Map<String, MasteryCategory> categories;
    private double globalMultiplier = 1.0d;

    public MasteryService(Map<String, MasteryCategory> categories) {
        this.categories = Map.copyOf(categories);
    }

    public static Map<String, MasteryCategory> parse(Cfg cfg) {
        Cfg section = cfg.section("categories");
        Map<String, MasteryCategory> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, Cfg> entry : section.childSections().entrySet()) {
            String id = Ids.normalize(entry.getKey());
            Cfg category = entry.getValue();
            XpCurve curve = XpCurve.fromConfig(category.section("xp-curve"));
            List<MasteryUnlock> unlocks = new ArrayList<>();
            Cfg unlocksSection = category.sectionOrEmpty("unlocks");
            for (Map.Entry<String, Cfg> unlockEntry : unlocksSection.childSections().entrySet()) {
                Cfg unlockCfg = unlockEntry.getValue();
                int level = unlockCfg.getInt("level");
                if (level < 1 || level > curve.maxLevel()) {
                    throw new ConfigException(unlockCfg.path() + ".level",
                            "must be between 1 and " + curve.maxLevel());
                }
                unlocks.add(new MasteryUnlock(
                        Ids.normalize(unlockEntry.getKey()),
                        level,
                        Ids.normalize(unlockCfg.getString("type", "passive")),
                        unlockCfg.getDouble("value", 0.0d),
                        unlockCfg.getString("label", unlockEntry.getKey()),
                        unlockCfg.getStringList("description")));
            }
            Map<String, Double> sources = new LinkedHashMap<>();
            Cfg sourcesSection = category.sectionOrEmpty("xp-sources");
            for (String key : sourcesSection.keys()) {
                sources.put(key.toLowerCase(Locale.ROOT), Math.max(0.0d, sourcesSection.getDouble(key)));
            }
            parsed.put(id, new MasteryCategory(
                    id,
                    category.getString("display-name", id),
                    category.getString("icon", "BOOK"),
                    curve,
                    unlocks,
                    sources));
        }
        if (parsed.isEmpty()) {
            throw new ConfigException(section.path(), "at least one mastery category must be configured");
        }
        return parsed;
    }

    public void reload(Map<String, MasteryCategory> categories) {
        this.categories = Map.copyOf(categories);
    }

    public void globalMultiplier(double multiplier) {
        this.globalMultiplier = Math.max(0.0d, multiplier);
    }

    public Collection<MasteryCategory> categories() {
        return categories.values();
    }

    public Optional<MasteryCategory> category(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(categories.get(id.toLowerCase(Locale.ROOT)));
    }

    public boolean contains(String id) {
        return category(id).isPresent();
    }

    public int levelOf(IslandProfile island, String categoryId) {
        return category(categoryId)
                .map(category -> category.curve().levelFor(island.masteryXp(category.id())))
                .orElse(0);
    }

    public LevelTrack track(IslandProfile island, String categoryId) {
        MasteryCategory category = require(categoryId);
        return new LevelTrack(category.curve(), island.masteryXp(category.id()));
    }

    public LevelUpResult addXp(IslandProfile island, String categoryId, long amount) {
        MasteryCategory category = require(categoryId);
        LevelTrack track = track(island, category.id());
        LevelUpResult result = track.addXp(Numbers.applyMultiplier(amount, globalMultiplier));
        if (result.changed()) {
            island.masteryXp(category.id(), track.totalXp());
        }
        return result;
    }

    public LevelUpResult addXpFromSource(IslandProfile island, String categoryId, String sourceId, long units) {
        MasteryCategory category = require(categoryId);
        long amount = (long) Math.floor(category.xpPerSource(sourceId) * Math.max(0L, units));
        return addXp(island, category.id(), amount);
    }

    public LevelUpResult setLevel(IslandProfile island, String categoryId, int level) {
        MasteryCategory category = require(categoryId);
        LevelTrack track = track(island, category.id());
        LevelUpResult result = track.setLevel(level);
        island.masteryXp(category.id(), track.totalXp());
        return result;
    }

    public LevelUpResult setXp(IslandProfile island, String categoryId, long xp) {
        MasteryCategory category = require(categoryId);
        LevelTrack track = track(island, category.id());
        LevelUpResult result = track.setTotalXp(xp);
        island.masteryXp(category.id(), track.totalXp());
        return result;
    }

    public boolean hasUnlock(IslandProfile island, String categoryId, String unlockId) {
        Optional<MasteryCategory> category = category(categoryId);
        if (category.isEmpty()) {
            return false;
        }
        String normalised = Ids.normalize(unlockId);
        int level = levelOf(island, categoryId);
        return category.get().unlocksUpTo(level).stream().anyMatch(unlock -> unlock.id().equals(normalised));
    }

    /** Total passive magnitude of a type across one category. */
    public double passiveValue(IslandProfile island, String categoryId, String type) {
        return category(categoryId)
                .map(category -> category.passiveValue(levelOf(island, categoryId), type))
                .orElse(0.0d);
    }

    /** Total passive magnitude of a type across every category. */
    public double totalPassiveValue(IslandProfile island, String type) {
        double total = 0.0d;
        for (MasteryCategory category : categories.values()) {
            total += category.passiveValue(levelOf(island, category.id()), type);
        }
        return total;
    }

    public List<MasteryUnlock> unlockedIn(IslandProfile island, String categoryId) {
        return category(categoryId)
                .map(category -> category.unlocksUpTo(levelOf(island, categoryId)))
                .orElse(List.of());
    }

    private MasteryCategory require(String id) {
        return category(id).orElseThrow(() -> new IllegalArgumentException("unknown mastery category: " + id));
    }
}
