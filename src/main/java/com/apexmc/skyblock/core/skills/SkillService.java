package com.apexmc.skyblock.core.skills;

import com.apexmc.skyblock.core.currency.TokenService;
import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.mastery.MasteryService;
import com.apexmc.skyblock.core.omnitool.OmniToolService;
import com.apexmc.skyblock.core.progression.IslandProgressionService;
import com.apexmc.skyblock.core.roles.RoleService;
import com.apexmc.skyblock.core.util.Numbers;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Turns a raw gameplay action (block broken, crop harvested, fish caught, mob
 * killed) into role XP, island XP, mastery XP, tokens, rare drops and
 * milestones. This is the heart of the progression loop and is entirely
 * Bukkit-free so it can be unit tested exhaustively.
 */
public final class SkillService {

    /** Hard safety cap: a single action can never be worth more than this many units. */
    public static final int MAX_UNITS_PER_ACTION = 64;

    private SkillRegistry registry;
    private final RoleService roles;
    private final IslandProgressionService islands;
    private final MasteryService mastery;
    private final TokenService tokens;
    private final OmniToolService omniTools;

    public SkillService(SkillRegistry registry,
                        RoleService roles,
                        IslandProgressionService islands,
                        MasteryService mastery,
                        TokenService tokens,
                        OmniToolService omniTools) {
        this.registry = registry;
        this.roles = roles;
        this.islands = islands;
        this.mastery = mastery;
        this.tokens = tokens;
        this.omniTools = omniTools;
    }

    public void reload(SkillRegistry registry) {
        this.registry = registry;
    }

    public SkillRegistry registry() {
        return registry;
    }

    /**
     * Applies one gameplay action.
     *
     * @param units number of targets (e.g. blocks); clamped to {@link #MAX_UNITS_PER_ACTION}
     */
    public SkillActionResult handle(PlayerProfile profile,
                                    IslandProfile island,
                                    SkillType type,
                                    String key,
                                    int units,
                                    RandomGenerator random) {
        Optional<SkillDefinition> maybeSkill = registry.skill(type);
        if (maybeSkill.isEmpty() || units <= 0) {
            return SkillActionResult.unhandled();
        }
        SkillDefinition skill = maybeSkill.get();
        Optional<SkillSource> maybeSource = skill.source(key);
        if (maybeSource.isEmpty()) {
            return SkillActionResult.unhandled();
        }
        SkillSource source = maybeSource.get();
        int islandLevel = islands.levelOf(island);
        if (islandLevel < source.requiredIslandLevel()) {
            return SkillActionResult.blocked("island-level");
        }

        int applied = Numbers.clamp(units, 1, MAX_UNITS_PER_ACTION);
        String toolId = omniTools.toolForRole(skill.roleId()).map(tool -> tool.id()).orElse(skill.roleId());

        double xpBonus = 1.0d
                + omniTools.effectByType(profile, toolId, "xp_bonus")
                + mastery.passiveValue(island, skill.masteryCategoryId(), "xp_bonus");

        long roleXp = Numbers.applyMultiplier(source.roleXp() * (long) applied, xpBonus);
        long islandXp = Numbers.applyMultiplier(source.islandXp() * (long) applied, xpBonus);
        long masteryXp = Numbers.applyMultiplier(source.masteryXp() * (long) applied, xpBonus);

        long tokenGain = 0L;
        double tokenChance = Numbers.clamp(source.tokenChance()
                + omniTools.effectByType(profile, toolId, "token_find")
                + mastery.passiveValue(island, skill.masteryCategoryId(), "token_find"), 0.0d, 1.0d);
        if (source.tokenAmount() > 0 && tokenChance > 0) {
            for (int i = 0; i < applied; i++) {
                if (random.nextDouble() < tokenChance) {
                    tokenGain = Numbers.safeAdd(tokenGain, source.tokenAmount());
                }
            }
        }

        List<SkillDrop> drops = new ArrayList<>();
        double dropChance = Numbers.clamp(skill.rareDropChance()
                + omniTools.effectByType(profile, toolId, "rare_drop")
                + mastery.passiveValue(island, skill.masteryCategoryId(), "rare_drop"), 0.0d, 1.0d);
        if (!skill.rareDrops().isEmpty() && dropChance > 0) {
            for (int i = 0; i < applied; i++) {
                if (random.nextDouble() < dropChance) {
                    skill.rareDrops().pick(random)
                            .filter(drop -> islandLevel >= drop.requiredIslandLevel())
                            .ifPresent(drops::add);
                }
            }
        }
        for (SkillDrop drop : drops) {
            tokenGain = Numbers.safeAdd(tokenGain, drop.tokens());
        }

        // Apply progression.
        if (roleXp > 0) {
            roles.addXp(profile, skill.roleId(), roleXp);
        }
        if (islandXp > 0) {
            islands.addXp(island, islandXp);
        }
        if (masteryXp > 0 && mastery.contains(skill.masteryCategoryId())) {
            mastery.addXp(island, skill.masteryCategoryId(), masteryXp);
        }
        if (tokenGain > 0) {
            tokens.give(profile, tokenGain);
        }

        List<SkillMilestone> crossed = applyMilestones(profile, island, skill, applied);

        return new SkillActionResult(true, "ok",
                new SkillReward(roleXp, islandXp, masteryXp, tokenGain), drops, crossed);
    }

    /** Statistic key used for milestone tracking, e.g. {@code mining_count}. */
    public static String statKey(SkillType type) {
        return type.id() + "_count";
    }

    private List<SkillMilestone> applyMilestones(PlayerProfile profile, IslandProfile island,
                                                 SkillDefinition skill, int applied) {
        String stat = statKey(skill.type());
        long before = profile.stat(stat);
        long after = Numbers.safeAdd(before, applied);
        profile.stat(stat, after);

        List<SkillMilestone> crossed = new ArrayList<>();
        for (SkillMilestone milestone : skill.milestones()) {
            if (before < milestone.amount() && after >= milestone.amount()) {
                crossed.add(milestone);
                if (milestone.tokenReward() > 0) {
                    tokens.give(profile, milestone.tokenReward());
                }
                if (milestone.islandXpReward() > 0) {
                    islands.addXp(island, milestone.islandXpReward());
                }
                island.grantedUnlocks().addAll(milestone.unlocks());
                if (!milestone.unlocks().isEmpty()) {
                    island.markDirty();
                }
            }
        }
        return crossed;
    }
}
