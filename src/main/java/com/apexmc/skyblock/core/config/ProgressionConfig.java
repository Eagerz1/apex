package com.apexmc.skyblock.core.config;

import com.apexmc.skyblock.core.companions.CompanionDefinition;
import com.apexmc.skyblock.core.companions.CompanionRarity;
import com.apexmc.skyblock.core.generators.GeneratorTier;
import com.apexmc.skyblock.core.generators.GeneratorType;
import com.apexmc.skyblock.core.mastery.MasteryCategory;
import com.apexmc.skyblock.core.omnitool.OmniToolDefinition;
import com.apexmc.skyblock.core.omnitool.UpgradeNode;
import com.apexmc.skyblock.core.progression.IslandLevelConfig;
import com.apexmc.skyblock.core.roles.RoleDefinition;
import com.apexmc.skyblock.core.roles.RoleRegistry;
import com.apexmc.skyblock.core.season.SeasonConfig;
import com.apexmc.skyblock.core.skills.SkillDefinition;
import com.apexmc.skyblock.core.skills.SkillRegistry;
import com.apexmc.skyblock.core.spawners.SpawnerTier;
import com.apexmc.skyblock.core.spawners.SpawnerType;
import java.util.Map;

/**
 * Fully parsed and cross-validated plugin configuration. Nothing outside this
 * class reads raw configuration maps, and no balance value is hard coded in
 * Java.
 */
public record ProgressionConfig(IslandLevelConfig islandLevels,
                                Map<String, MasteryCategory> mastery,
                                RoleRegistry roles,
                                Map<String, OmniToolDefinition> omniTools,
                                SkillRegistry skills,
                                Map<String, GeneratorType> generators,
                                Map<String, SpawnerType> spawners,
                                Map<String, CompanionRarity> companionRarities,
                                Map<String, CompanionDefinition> companions,
                                int maxCompanionsPerPlayer,
                                double companionMergeXpCarry,
                                SeasonConfig season,
                                CurrencySettings currency,
                                com.apexmc.skyblock.core.economy.EconomyGateway.Policy missingEconomyPolicy,
                                Messages messages,
                                ValidationReport report) {

    /** Sky Token display settings (the currency can be renamed entirely). */
    public record CurrencySettings(String displayName, String displayNamePlural, String symbol,
                                   long maxBalance, String placeholderName) {
    }

    /**
     * Parses every section and cross-validates identifiers. Throws
     * {@link ConfigException} with a useful path when something is wrong.
     */
    public static ProgressionConfig parse(Cfg root) {
        IslandLevelConfig islandLevels = IslandLevelConfig.fromConfig(root.section("island"));
        Map<String, MasteryCategory> mastery = com.apexmc.skyblock.core.mastery.MasteryService.parse(root.section("mastery"));
        RoleRegistry roles = RoleRegistry.fromConfig(root.section("roles-config"));
        Map<String, OmniToolDefinition> omniTools = com.apexmc.skyblock.core.omnitool.OmniToolService.parse(root.section("omnitools-config"));
        SkillRegistry skills = SkillRegistry.fromConfig(root.section("skills"));
        Map<String, GeneratorType> generators = com.apexmc.skyblock.core.generators.GeneratorService.parse(root.section("generators-config"));
        Map<String, SpawnerType> spawners = com.apexmc.skyblock.core.spawners.SpawnerService.parse(root.section("spawners-config"));

        Cfg companionSection = root.section("companions-config");
        Map<String, CompanionRarity> rarities = com.apexmc.skyblock.core.companions.CompanionService.parseRarities(companionSection);
        Map<String, CompanionDefinition> companions =
                com.apexmc.skyblock.core.companions.CompanionService.parseDefinitions(companionSection, rarities);
        int maxCompanions = Math.max(1, companionSection.getInt("max-per-player", 9));
        double mergeCarry = companionSection.getDouble("merge-xp-carry", 0.5d);
        if (mergeCarry < 0 || mergeCarry > 1) {
            throw new ConfigException(companionSection.path() + ".merge-xp-carry", "must be between 0.0 and 1.0");
        }

        SeasonConfig season = SeasonConfig.fromConfig(root.section("season"));
        Cfg currencyCfg = root.section("currency");
        CurrencySettings currency = new CurrencySettings(
                currencyCfg.getString("display-name", "Sky Token"),
                currencyCfg.getString("display-name-plural", "Sky Tokens"),
                currencyCfg.getString("symbol", "\u2726"),
                currencyCfg.getLong("max-balance", 1_000_000_000L),
                currencyCfg.getString("placeholder-name", "skytokens"));
        Messages messages = Messages.fromConfig(root.sectionOrEmpty("messages"));

        Cfg economyCfg = root.sectionOrEmpty("economy");
        com.apexmc.skyblock.core.economy.EconomyGateway.Policy economyPolicy;
        String behaviour = economyCfg.getString("missing-vault-behaviour", "waive");
        try {
            economyPolicy = com.apexmc.skyblock.core.economy.EconomyGateway.Policy.fromId(behaviour);
        } catch (IllegalArgumentException ex) {
            throw new ConfigException(economyCfg.path() + ".missing-vault-behaviour",
                    "must be 'waive' or 'block' (got '" + behaviour + "')");
        }

        ValidationReport report = new ValidationReport();
        validate(report, islandLevels, mastery, roles, omniTools, skills, generators, spawners, rarities, companions);
        report.throwIfInvalid();

        return new ProgressionConfig(islandLevels, mastery, roles, omniTools, skills, generators, spawners,
                rarities, companions, maxCompanions, mergeCarry, season, currency, economyPolicy, messages, report);
    }

    private static void validate(ValidationReport report,
                                 IslandLevelConfig islandLevels,
                                 Map<String, MasteryCategory> mastery,
                                 RoleRegistry roles,
                                 Map<String, OmniToolDefinition> omniTools,
                                 SkillRegistry skills,
                                 Map<String, GeneratorType> generators,
                                 Map<String, SpawnerType> spawners,
                                 Map<String, CompanionRarity> rarities,
                                 Map<String, CompanionDefinition> companions) {
        for (RoleDefinition role : roles.all()) {
            if (!omniTools.containsKey(role.omniToolId())) {
                report.error("roles-config.roles." + role.id() + ".omnitool",
                        "unknown omnitool '" + role.omniToolId() + "'");
            }
            if (role.requiredIslandLevel() > islandLevels.maxLevel()) {
                report.error("roles-config.roles." + role.id() + ".required-island-level",
                        "island level " + role.requiredIslandLevel() + " exceeds the configured maximum ("
                                + islandLevels.maxLevel() + ")");
            }
        }
        for (OmniToolDefinition tool : omniTools.values()) {
            if (!roles.contains(tool.roleId())) {
                report.error("omnitools-config.omnitools." + tool.id() + ".role",
                        "unknown role '" + tool.roleId() + "'");
            }
            for (UpgradeNode node : tool.nodes().values()) {
                checkUnlock(report, islandLevels, "omnitools-config.omnitools." + tool.id() + ".nodes." + node.id(),
                        node.requiredUnlock());
                if (node.requiredIslandLevel() > islandLevels.maxLevel()) {
                    report.error("omnitools-config.omnitools." + tool.id() + ".nodes." + node.id() + ".required-island-level",
                            "exceeds the configured island level maximum");
                }
            }
        }
        for (SkillDefinition skill : skills.all()) {
            if (!roles.contains(skill.roleId())) {
                report.error("skills." + skill.type().id() + ".role", "unknown role '" + skill.roleId() + "'");
            }
            if (!mastery.containsKey(skill.masteryCategoryId())) {
                report.error("skills." + skill.type().id() + ".mastery-category",
                        "unknown mastery category '" + skill.masteryCategoryId() + "'");
            }
        }
        for (GeneratorType generator : generators.values()) {
            for (GeneratorTier tier : generator.tiers()) {
                checkUnlock(report, islandLevels, "generators-config.generators." + generator.id() + ".tiers." + tier.tier(),
                        tier.requiredUnlock());
            }
        }
        for (SpawnerType spawner : spawners.values()) {
            for (SpawnerTier tier : spawner.tiers()) {
                checkUnlock(report, islandLevels, "spawners-config.spawners." + spawner.id() + ".tiers." + tier.tier(),
                        tier.requiredUnlock());
            }
        }
        for (CompanionDefinition companion : companions.values()) {
            if (!rarities.containsKey(companion.baseRarityId())) {
                report.error("companions-config.companions." + companion.id() + ".rarity",
                        "unknown rarity '" + companion.baseRarityId() + "'");
            }
        }
        if (islandLevels.xpSources().isEmpty()) {
            report.warn("island.xp-sources", "no island XP sources configured; islands can only gain XP from skills");
        }
    }

    private static void checkUnlock(ValidationReport report, IslandLevelConfig islandLevels, String path, String unlock) {
        if (unlock == null || unlock.isEmpty()) {
            return;
        }
        if (islandLevels.unlockLevel(unlock).isEmpty()) {
            report.error(path + ".required-unlock",
                    "unlock '" + unlock + "' is not granted by any island level");
        }
    }
}
