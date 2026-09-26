package com.apexmc.skyblock.core.skills;

import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.core.config.ConfigException;
import com.apexmc.skyblock.core.util.Ids;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Parses and stores the mining / farming / fishing / slayer / logging configuration. */
public final class SkillRegistry {

    private final Map<SkillType, SkillDefinition> skills;

    private SkillRegistry(Map<SkillType, SkillDefinition> skills) {
        this.skills = new EnumMap<>(skills);
    }

    public static SkillRegistry fromConfig(Cfg cfg) {
        Map<SkillType, SkillDefinition> parsed = new EnumMap<>(SkillType.class);
        for (Map.Entry<String, Cfg> entry : cfg.childSections().entrySet()) {
            SkillType type;
            try {
                type = SkillType.fromId(entry.getKey());
            } catch (IllegalArgumentException ex) {
                throw new ConfigException(cfg.path() + "." + entry.getKey(),
                        "unknown skill (expected mining, farming, fishing, slayer or logging)");
            }
            Cfg skill = entry.getValue();

            Map<String, SkillSource> sources = new LinkedHashMap<>();
            Cfg sourcesSection = skill.sectionOrEmpty("sources");
            for (Map.Entry<String, Cfg> sourceEntry : sourcesSection.childSections().entrySet()) {
                Cfg source = sourceEntry.getValue();
                String key = Ids.key(sourceEntry.getKey());
                double tokenChance = source.getDouble("token-chance", 0.0d);
                if (tokenChance < 0 || tokenChance > 1) {
                    throw new ConfigException(source.path() + ".token-chance", "must be between 0.0 and 1.0");
                }
                sources.put(key, new SkillSource(
                        key,
                        Math.max(0L, source.getLong("role-xp", 0L)),
                        Math.max(0L, source.getLong("island-xp", 0L)),
                        Math.max(0L, source.getLong("mastery-xp", 0L)),
                        Math.max(0, source.getInt("required-island-level", 0)),
                        tokenChance,
                        Math.max(0L, source.getLong("token-amount", 0L))));
            }
            if (sources.isEmpty()) {
                throw new ConfigException(skill.path() + ".sources", "skill '" + type.id() + "' has no configured sources");
            }

            List<WeightedTable.Entry<SkillDrop>> drops = new ArrayList<>();
            Cfg dropsSection = skill.sectionOrEmpty("rare-drops");
            for (Map.Entry<String, Cfg> dropEntry : dropsSection.childSections().entrySet()) {
                Cfg drop = dropEntry.getValue();
                double weight = drop.getDouble("weight", 1.0d);
                if (weight < 0) {
                    throw new ConfigException(drop.path() + ".weight", "must not be negative");
                }
                drops.add(new WeightedTable.Entry<>(new SkillDrop(
                        Ids.normalize(dropEntry.getKey()),
                        Ids.key(drop.getString("material", "DIAMOND")),
                        Math.max(1, drop.getInt("amount", 1)),
                        Math.max(0, drop.getInt("required-island-level", 0)),
                        Math.max(0L, drop.getLong("tokens", 0L))), weight));
            }

            double rareChance = skill.getDouble("rare-drop-chance", 0.0d);
            if (rareChance < 0 || rareChance > 1) {
                throw new ConfigException(skill.path() + ".rare-drop-chance", "must be between 0.0 and 1.0");
            }

            List<SkillMilestone> milestones = new ArrayList<>();
            Cfg milestoneSection = skill.sectionOrEmpty("milestones");
            for (Map.Entry<String, Cfg> milestoneEntry : milestoneSection.childSections().entrySet()) {
                Cfg milestone = milestoneEntry.getValue();
                long amount = milestone.getLong("amount");
                if (amount <= 0) {
                    throw new ConfigException(milestone.path() + ".amount", "must be greater than zero");
                }
                List<String> unlocks = new ArrayList<>();
                for (String unlock : milestone.getStringList("unlocks")) {
                    unlocks.add(Ids.normalize(unlock));
                }
                milestones.add(new SkillMilestone(
                        Ids.normalize(milestoneEntry.getKey()),
                        amount,
                        Math.max(0L, milestone.getLong("tokens", 0L)),
                        Math.max(0L, milestone.getLong("island-xp", 0L)),
                        unlocks));
            }
            milestones.sort(java.util.Comparator.comparingLong(SkillMilestone::amount));

            parsed.put(type, new SkillDefinition(
                    type,
                    Ids.normalize(skill.getString("role", type.id())),
                    Ids.normalize(skill.getString("mastery-category", type.id())),
                    sources,
                    new WeightedTable<>(drops),
                    rareChance,
                    milestones));
        }
        if (parsed.isEmpty()) {
            throw new ConfigException(cfg.path(), "no skills configured");
        }
        return new SkillRegistry(parsed);
    }

    public Optional<SkillDefinition> skill(SkillType type) {
        return Optional.ofNullable(skills.get(type));
    }

    public Collection<SkillDefinition> all() {
        return skills.values();
    }

    /** Finds the skill that handles a given material/entity key, if any. */
    public Optional<SkillDefinition> skillForKey(SkillType type, String key) {
        return skill(type).filter(definition -> definition.source(key).isPresent());
    }
}
