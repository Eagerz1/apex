package com.apexmc.skyblock.core.service;

import com.apexmc.skyblock.core.companions.CompanionService;
import com.apexmc.skyblock.core.config.ProgressionConfig;
import com.apexmc.skyblock.core.currency.TokenService;
import com.apexmc.skyblock.core.economy.DisabledEconomy;
import com.apexmc.skyblock.core.economy.EconomyBridge;
import com.apexmc.skyblock.core.generators.GeneratorService;
import com.apexmc.skyblock.core.mastery.MasteryService;
import com.apexmc.skyblock.core.omnitool.OmniToolService;
import com.apexmc.skyblock.core.progression.IslandProgressionService;
import com.apexmc.skyblock.core.roles.RoleService;
import com.apexmc.skyblock.core.season.SeasonService;
import com.apexmc.skyblock.core.skills.SkillService;
import com.apexmc.skyblock.core.spawners.SpawnerService;

/**
 * Wires every core service together. Reloading configuration swaps the parsed
 * definitions in place, keeping runtime state (placed generators, cached
 * profiles) intact.
 */
public final class ProgressionServices {

    private ProgressionConfig config;
    private EconomyBridge economy;

    private final TokenService tokens;
    private final IslandProgressionService islands;
    private final MasteryService mastery;
    private final RoleService roles;
    private final OmniToolService omniTools;
    private final SkillService skills;
    private final GeneratorService generators;
    private final SpawnerService spawners;
    private final CompanionService companions;
    private final SeasonService seasons;

    public ProgressionServices(ProgressionConfig config, EconomyBridge economy) {
        this.config = config;
        this.economy = economy == null ? new DisabledEconomy() : economy;
        this.tokens = new TokenService(config.currency().displayName(), config.currency().displayNamePlural(),
                config.currency().symbol(), config.currency().maxBalance());
        this.islands = new IslandProgressionService(config.islandLevels());
        this.mastery = new MasteryService(config.mastery());
        this.roles = new RoleService(config.roles());
        this.omniTools = new OmniToolService(config.omniTools(), tokens, this.economy);
        this.skills = new SkillService(config.skills(), roles, islands, mastery, tokens, omniTools);
        this.generators = new GeneratorService(config.generators(), tokens, islands, mastery, this.economy);
        this.spawners = new SpawnerService(config.spawners(), tokens, islands, this.economy);
        this.companions = new CompanionService(config.companions(), config.companionRarities(), tokens,
                config.maxCompanionsPerPlayer());
        this.seasons = new SeasonService(config.season());
        applySeasonMultipliers();
    }

    /** Swaps in a freshly parsed configuration without dropping runtime state. */
    public void reload(ProgressionConfig config) {
        this.config = config;
        tokens.rename(config.currency().displayName(), config.currency().displayNamePlural(),
                config.currency().symbol(), config.currency().maxBalance());
        islands.reload(config.islandLevels());
        mastery.reload(config.mastery());
        roles.reload(config.roles());
        omniTools.reload(config.omniTools());
        skills.reload(config.skills());
        generators.reload(config.generators());
        spawners.reload(config.spawners());
        companions.reload(config.companions(), config.companionRarities(), config.maxCompanionsPerPlayer());
        seasons.reload(config.season());
        applySeasonMultipliers();
    }

    public void economy(EconomyBridge economy) {
        this.economy = economy == null ? new DisabledEconomy() : economy;
        omniTools.economy(this.economy);
        generators.economy(this.economy);
        spawners.economy(this.economy);
    }

    private void applySeasonMultipliers() {
        islands.globalMultiplier(seasons.xpMultiplier());
        roles.globalMultiplier(seasons.xpMultiplier());
        mastery.globalMultiplier(seasons.xpMultiplier());
    }

    public ProgressionConfig config() {
        return config;
    }

    public EconomyBridge economy() {
        return economy;
    }

    public TokenService tokens() {
        return tokens;
    }

    public IslandProgressionService islands() {
        return islands;
    }

    public MasteryService mastery() {
        return mastery;
    }

    public RoleService roles() {
        return roles;
    }

    public OmniToolService omniTools() {
        return omniTools;
    }

    public SkillService skills() {
        return skills;
    }

    public GeneratorService generators() {
        return generators;
    }

    public SpawnerService spawners() {
        return spawners;
    }

    public CompanionService companions() {
        return companions;
    }

    public SeasonService seasons() {
        return seasons;
    }
}
