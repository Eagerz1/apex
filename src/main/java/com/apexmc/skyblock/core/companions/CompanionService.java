package com.apexmc.skyblock.core.companions;

import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.core.config.ConfigException;
import com.apexmc.skyblock.core.currency.TokenService;
import com.apexmc.skyblock.core.data.CompanionInstance;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.progression.LevelTrack;
import com.apexmc.skyblock.core.progression.LevelUpResult;
import com.apexmc.skyblock.core.progression.XpCurve;
import com.apexmc.skyblock.core.util.Ids;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Companions/pets: rarities, levels, abilities and safe merging.
 *
 * <p>Companion instances are owned by the player profile and identified by a
 * server generated UUID, so item based duplication cannot create progression.</p>
 */
public final class CompanionService {

    private Map<String, CompanionDefinition> definitions;
    private Map<String, CompanionRarity> rarities;
    private final TokenService tokens;
    private int maxCompanionsPerPlayer;

    public CompanionService(Map<String, CompanionDefinition> definitions,
                            Map<String, CompanionRarity> rarities,
                            TokenService tokens,
                            int maxCompanionsPerPlayer) {
        this.definitions = Map.copyOf(definitions);
        this.rarities = Map.copyOf(rarities);
        this.tokens = tokens;
        this.maxCompanionsPerPlayer = Math.max(1, maxCompanionsPerPlayer);
    }

    /** Parses the {@code rarities} section. */
    public static Map<String, CompanionRarity> parseRarities(Cfg cfg) {
        Cfg section = cfg.section("rarities");
        Map<String, CompanionRarity> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, Cfg> entry : section.childSections().entrySet()) {
            String id = Ids.normalize(entry.getKey());
            Cfg rarity = entry.getValue();
            int maxLevel = rarity.getInt("max-level");
            if (maxLevel < 1 || maxLevel > 10_000) {
                throw new ConfigException(rarity.path() + ".max-level", "must be between 1 and 10000");
            }
            Cfg curveCfg = rarity.sectionOrEmpty("xp-curve");
            XpCurve curve;
            if (curveCfg.isEmpty()) {
                curve = XpCurve.geometric(maxLevel, rarity.getLong("xp-base", 100L), rarity.getDouble("xp-growth", 1.08d));
            } else {
                curve = XpCurve.fromConfig(curveCfg);
                if (curve.maxLevel() != maxLevel) {
                    throw new ConfigException(curveCfg.path(),
                            "xp curve max level (" + curve.maxLevel() + ") must match max-level (" + maxLevel + ")");
                }
            }
            parsed.put(id, new CompanionRarity(
                    id,
                    rarity.getString("display-name", id),
                    rarity.getString("colour", "&7"),
                    maxLevel,
                    curve,
                    rarity.getDouble("ability-scale", 1.0d),
                    Math.max(0L, rarity.getLong("merge-cost", 0L)),
                    rarity.getString("next-rarity", "").trim().toLowerCase(Locale.ROOT)));
        }
        if (parsed.isEmpty()) {
            throw new ConfigException(section.path(), "at least one companion rarity must be configured");
        }
        for (CompanionRarity rarity : parsed.values()) {
            if (!rarity.nextRarityId().isEmpty() && !parsed.containsKey(rarity.nextRarityId())) {
                throw new ConfigException(section.path() + "." + rarity.id() + ".next-rarity",
                        "unknown rarity '" + rarity.nextRarityId() + "'");
            }
        }
        return parsed;
    }

    /** Parses the {@code companions} section. */
    public static Map<String, CompanionDefinition> parseDefinitions(Cfg cfg, Map<String, CompanionRarity> rarities) {
        Cfg section = cfg.section("companions");
        Map<String, CompanionDefinition> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, Cfg> entry : section.childSections().entrySet()) {
            String id = Ids.normalize(entry.getKey());
            Cfg companion = entry.getValue();
            String rarityId = Ids.normalize(companion.getString("rarity", "common"));
            if (!rarities.containsKey(rarityId)) {
                throw new ConfigException(companion.path() + ".rarity", "unknown rarity '" + rarityId + "'");
            }
            parsed.put(id, new CompanionDefinition(
                    id,
                    companion.getString("display-name", id),
                    Ids.key(companion.getString("icon", "BONE")),
                    Ids.normalize(companion.getString("ability", "island_xp_bonus")),
                    companion.getDouble("ability-base", 0.0d),
                    companion.getDouble("ability-per-level", 0.0d),
                    rarityId,
                    companion.getStringList("description"),
                    Math.max(0, companion.getInt("required-island-level", 0))));
        }
        if (parsed.isEmpty()) {
            throw new ConfigException(section.path(), "at least one companion must be configured");
        }
        return parsed;
    }

    public void reload(Map<String, CompanionDefinition> definitions, Map<String, CompanionRarity> rarities, int maxPerPlayer) {
        this.definitions = Map.copyOf(definitions);
        this.rarities = Map.copyOf(rarities);
        this.maxCompanionsPerPlayer = Math.max(1, maxPerPlayer);
    }

    public Collection<CompanionDefinition> definitions() {
        return definitions.values();
    }

    public Collection<CompanionRarity> rarities() {
        return rarities.values();
    }

    public Optional<CompanionDefinition> definition(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(definitions.get(id.toLowerCase(Locale.ROOT)));
    }

    public Optional<CompanionRarity> rarity(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(rarities.get(id.toLowerCase(Locale.ROOT)));
    }

    public int level(CompanionInstance companion) {
        return rarity(companion.rarityId()).map(rarity -> rarity.curve().levelFor(companion.xp())).orElse(0);
    }

    public int maxLevel(CompanionInstance companion) {
        return rarity(companion.rarityId()).map(CompanionRarity::maxLevel).orElse(0);
    }

    /** Ability magnitude of a companion at its current level and rarity. */
    public double abilityValue(CompanionInstance companion) {
        Optional<CompanionDefinition> definition = definition(companion.definitionId());
        Optional<CompanionRarity> rarity = rarity(companion.rarityId());
        if (definition.isEmpty() || rarity.isEmpty()) {
            return 0.0d;
        }
        int level = level(companion);
        double raw = definition.get().abilityBase() + definition.get().abilityPerLevel() * level;
        return raw * rarity.get().abilityScale();
    }

    /** Ability magnitude of the player's active companion for a given ability id. */
    public double activeAbility(PlayerProfile profile, String ability) {
        return activeCompanion(profile)
                .filter(companion -> definition(companion.definitionId())
                        .map(definition -> definition.ability().equalsIgnoreCase(ability)).orElse(false))
                .map(this::abilityValue)
                .orElse(0.0d);
    }

    public Optional<CompanionInstance> activeCompanion(PlayerProfile profile) {
        return profile.activeCompanionId().flatMap(profile::companion);
    }

    /** Result of granting or merging a companion. */
    public record CompanionResult(boolean success, String reason, CompanionInstance companion) {
        public static CompanionResult fail(String reason) {
            return new CompanionResult(false, reason, null);
        }
    }

    /** Grants a new companion to a player (drops, rewards, admin commands). */
    public CompanionResult grant(PlayerProfile profile, String definitionId, String rarityId, int islandLevel) {
        Optional<CompanionDefinition> definition = definition(definitionId);
        if (definition.isEmpty()) {
            return CompanionResult.fail("unknown-companion");
        }
        String resolvedRarity = rarityId == null || rarityId.isBlank()
                ? definition.get().baseRarityId() : Ids.normalize(rarityId);
        if (rarity(resolvedRarity).isEmpty()) {
            return CompanionResult.fail("unknown-rarity");
        }
        if (islandLevel < definition.get().requiredIslandLevel()) {
            return CompanionResult.fail("island-level");
        }
        if (profile.companions().size() >= maxCompanionsPerPlayer) {
            return CompanionResult.fail("companion-limit");
        }
        CompanionInstance instance = new CompanionInstance(UUID.randomUUID(), definition.get().id(), resolvedRarity, 0L);
        profile.companions().add(instance);
        if (profile.activeCompanionId().isEmpty()) {
            profile.activeCompanionId(instance.instanceId());
        }
        profile.markDirty();
        return new CompanionResult(true, "ok", instance);
    }

    public boolean setActive(PlayerProfile profile, UUID instanceId) {
        if (profile.companion(instanceId).isEmpty()) {
            return false;
        }
        profile.activeCompanionId(instanceId);
        return true;
    }

    /** Adds companion XP; capped by the rarity curve. */
    public LevelUpResult addXp(PlayerProfile profile, UUID instanceId, long amount) {
        Optional<CompanionInstance> maybe = profile.companion(instanceId);
        if (maybe.isEmpty()) {
            return new LevelUpResult(0, 0, 0L, 0L);
        }
        CompanionInstance companion = maybe.get();
        Optional<CompanionRarity> rarity = rarity(companion.rarityId());
        if (rarity.isEmpty()) {
            return new LevelUpResult(0, 0, companion.xp(), 0L);
        }
        LevelTrack track = new LevelTrack(rarity.get().curve(), companion.xp());
        LevelUpResult result = track.addXp(amount);
        if (result.changed()) {
            companion.xp(track.totalXp());
            profile.markDirty();
        }
        return result;
    }

    /**
     * Merges {@code sacrifice} into {@code target}.
     *
     * <p>Both companions must be the same type and rarity. The sacrifice is
     * destroyed, the target is promoted to the next rarity (when one exists) and
     * keeps a configurable share of the combined XP. The sacrifice is only
     * removed after every validation passed, so a failed merge can never delete
     * a companion.</p>
     */
    public CompanionResult merge(PlayerProfile profile, UUID targetId, UUID sacrificeId, double xpCarryRatio) {
        if (targetId == null || sacrificeId == null || targetId.equals(sacrificeId)) {
            return CompanionResult.fail("invalid-selection");
        }
        Optional<CompanionInstance> maybeTarget = profile.companion(targetId);
        Optional<CompanionInstance> maybeSacrifice = profile.companion(sacrificeId);
        if (maybeTarget.isEmpty() || maybeSacrifice.isEmpty()) {
            return CompanionResult.fail("unknown-companion");
        }
        CompanionInstance target = maybeTarget.get();
        CompanionInstance sacrifice = maybeSacrifice.get();
        if (!target.definitionId().equals(sacrifice.definitionId())) {
            return CompanionResult.fail("type-mismatch");
        }
        if (!target.rarityId().equals(sacrifice.rarityId())) {
            return CompanionResult.fail("rarity-mismatch");
        }
        CompanionRarity rarity = rarity(target.rarityId()).orElse(null);
        if (rarity == null) {
            return CompanionResult.fail("unknown-rarity");
        }
        if (rarity.nextRarityId().isEmpty()) {
            return CompanionResult.fail("max-rarity");
        }
        CompanionRarity next = rarity(rarity.nextRarityId()).orElse(null);
        if (next == null) {
            return CompanionResult.fail("unknown-rarity");
        }
        if (rarity.mergeCost() > 0 && !tokens.take(profile, rarity.mergeCost()).success()) {
            return CompanionResult.fail("insufficient-tokens");
        }
        double ratio = Math.max(0.0d, Math.min(1.0d, xpCarryRatio));
        long carried = (long) Math.floor((target.xp() + sacrifice.xp()) * ratio);
        profile.companions().remove(sacrifice);
        if (profile.activeCompanionId().filter(sacrificeId::equals).isPresent()) {
            profile.activeCompanionId(target.instanceId());
        }
        target.rarityId(next.id());
        target.xp(Math.min(carried, next.curve().totalXpCap()));
        profile.markDirty();
        return new CompanionResult(true, "ok", target);
    }

    /** Drops companions whose definition or rarity vanished from the configuration. */
    public List<CompanionInstance> sanitize(PlayerProfile profile) {
        List<CompanionInstance> removed = profile.companions().stream()
                .filter(companion -> definition(companion.definitionId()).isEmpty() || rarity(companion.rarityId()).isEmpty())
                .toList();
        if (!removed.isEmpty()) {
            profile.companions().removeAll(removed);
            profile.activeCompanionId().ifPresent(active -> {
                if (profile.companion(active).isEmpty()) {
                    profile.activeCompanionId(profile.companions().isEmpty()
                            ? null : profile.companions().get(0).instanceId());
                }
            });
            profile.markDirty();
        }
        return removed;
    }
}
