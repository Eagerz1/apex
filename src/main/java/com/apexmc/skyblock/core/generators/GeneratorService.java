package com.apexmc.skyblock.core.generators;

import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.core.config.ConfigException;
import com.apexmc.skyblock.core.currency.TokenService;
import com.apexmc.skyblock.core.data.BlockPosition;
import com.apexmc.skyblock.core.data.GeneratorState;
import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.economy.EconomyBridge;
import com.apexmc.skyblock.core.mastery.MasteryService;
import com.apexmc.skyblock.core.progression.IslandProgressionService;
import com.apexmc.skyblock.core.skills.WeightedTable;
import com.apexmc.skyblock.core.util.Ids;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Placement, ownership, upgrading and ticking of island generators.
 *
 * <p>Generators are tracked by block position so a generator can never be
 * duplicated by breaking and re-placing the block: placement registers state,
 * removal deletes it, and every mutation is validated against the island.</p>
 */
public final class GeneratorService {

    private Map<String, GeneratorType> types;
    private final TokenService tokens;
    private final IslandProgressionService islands;
    private final MasteryService mastery;
    private EconomyBridge economy;
    private String masteryCategoryId = "generators";

    private final Map<String, GeneratorState> byPosition = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, GeneratorState>> byIsland = new ConcurrentHashMap<>();

    public GeneratorService(Map<String, GeneratorType> types, TokenService tokens,
                            IslandProgressionService islands, MasteryService mastery, EconomyBridge economy) {
        this.types = Map.copyOf(types);
        this.tokens = tokens;
        this.islands = islands;
        this.mastery = mastery;
        this.economy = economy;
    }

    public static Map<String, GeneratorType> parse(Cfg cfg) {
        Cfg section = cfg.section("generators");
        Map<String, GeneratorType> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, Cfg> entry : section.childSections().entrySet()) {
            String id = Ids.normalize(entry.getKey());
            Cfg generator = entry.getValue();
            Cfg tiersSection = generator.section("tiers");
            Map<Integer, Cfg> ordered = new java.util.TreeMap<>();
            for (Map.Entry<String, Cfg> tierEntry : tiersSection.childSections().entrySet()) {
                try {
                    ordered.put(Integer.parseInt(tierEntry.getKey().trim()), tierEntry.getValue());
                } catch (NumberFormatException ex) {
                    throw new ConfigException(tiersSection.path() + "." + tierEntry.getKey(),
                            "tier keys must be whole numbers");
                }
            }
            if (ordered.isEmpty()) {
                throw new ConfigException(tiersSection.path(), "generator '" + id + "' must define at least one tier");
            }
            List<GeneratorTier> tiers = new ArrayList<>();
            int expected = 1;
            for (Map.Entry<Integer, Cfg> tierEntry : ordered.entrySet()) {
                if (tierEntry.getKey() != expected) {
                    throw new ConfigException(tiersSection.path(),
                            "generator tiers must be contiguous; expected " + expected + " but found " + tierEntry.getKey());
                }
                Cfg tier = tierEntry.getValue();
                int interval = tier.getInt("interval-ticks", 100);
                if (interval < 1) {
                    throw new ConfigException(tier.path() + ".interval-ticks", "must be at least 1 tick");
                }
                List<WeightedTable.Entry<String>> outputs = new ArrayList<>();
                Cfg outputSection = tier.sectionOrEmpty("outputs");
                for (String material : outputSection.keys()) {
                    outputs.add(new WeightedTable.Entry<>(Ids.key(material), outputSection.getDouble(material)));
                }
                if (outputs.isEmpty()) {
                    throw new ConfigException(tier.path() + ".outputs", "a generator tier must define at least one output");
                }
                tiers.add(new GeneratorTier(
                        expected,
                        tier.getString("display-name", id + " " + expected),
                        interval,
                        new WeightedTable<>(outputs),
                        Math.max(1, tier.getInt("output-amount", 1)),
                        Math.max(0, tier.getInt("required-island-level", 0)),
                        tier.getString("required-unlock", "").trim().toLowerCase(Locale.ROOT),
                        Math.max(0L, tier.getLong("token-cost", 0L)),
                        Math.max(0.0d, tier.getDouble("money-cost", 0.0d)),
                        Math.max(0L, tier.getLong("island-xp-per-cycle", 0L)),
                        Math.max(0L, tier.getLong("mastery-xp-per-cycle", 0L)),
                        tier.getStringList("abilities")));
                expected++;
            }
            parsed.put(id, new GeneratorType(
                    id,
                    generator.getString("display-name", id),
                    Ids.key(generator.getString("icon", "COBBLESTONE")),
                    Math.max(1, generator.getInt("max-per-island", 10)),
                    tiers));
        }
        if (parsed.isEmpty()) {
            throw new ConfigException(section.path(), "at least one generator type must be configured");
        }
        return parsed;
    }

    public void reload(Map<String, GeneratorType> types) {
        this.types = Map.copyOf(types);
    }

    public void economy(EconomyBridge economy) {
        this.economy = economy;
    }

    public void masteryCategoryId(String masteryCategoryId) {
        this.masteryCategoryId = masteryCategoryId;
    }

    public Collection<GeneratorType> types() {
        return types.values();
    }

    public Optional<GeneratorType> type(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(types.get(id.toLowerCase(Locale.ROOT)));
    }

    /** Loads persisted state (called once on startup, off the main thread). */
    public void load(Collection<GeneratorState> states) {
        byPosition.clear();
        byIsland.clear();
        states.forEach(this::index);
    }

    private void index(GeneratorState state) {
        byPosition.put(state.position().key(), state);
        byIsland.computeIfAbsent(state.islandId(), key -> new ConcurrentHashMap<>()).put(state.id(), state);
    }

    public Collection<GeneratorState> all() {
        return List.copyOf(byPosition.values());
    }

    public Collection<GeneratorState> ofIsland(UUID islandId) {
        return List.copyOf(byIsland.getOrDefault(islandId, Map.of()).values());
    }

    public int countOfType(UUID islandId, String typeId) {
        return (int) ofIsland(islandId).stream().filter(state -> state.typeId().equals(typeId)).count();
    }

    public Optional<GeneratorState> at(BlockPosition position) {
        return Optional.ofNullable(byPosition.get(position.key()));
    }

    /** Result of a placement attempt. */
    public record PlacementResult(boolean success, String reason, GeneratorState state) {
        public static PlacementResult fail(String reason) {
            return new PlacementResult(false, reason, null);
        }
    }

    /** Registers a newly placed generator after validating every requirement. */
    public PlacementResult place(IslandProfile island, PlayerProfile placer, String typeId, BlockPosition position, int tier) {
        Optional<GeneratorType> maybeType = type(typeId);
        if (maybeType.isEmpty()) {
            return PlacementResult.fail("unknown-type");
        }
        GeneratorType type = maybeType.get();
        if (byPosition.containsKey(position.key())) {
            return PlacementResult.fail("occupied");
        }
        if (countOfType(island.islandId(), type.id()) >= type.maxPerIsland()) {
            return PlacementResult.fail("limit-reached");
        }
        Optional<GeneratorTier> maybeTier = type.tier(tier);
        if (maybeTier.isEmpty()) {
            return PlacementResult.fail("unknown-tier");
        }
        GeneratorTier definition = maybeTier.get();
        if (islands.levelOf(island) < definition.requiredIslandLevel()) {
            return PlacementResult.fail("island-level");
        }
        if (!definition.requiredUnlock().isEmpty() && !islands.isUnlocked(island, definition.requiredUnlock())) {
            return PlacementResult.fail("locked");
        }
        GeneratorState state = new GeneratorState(UUID.randomUUID(), island.islandId(), position, type.id(), tier, 0L);
        index(state);
        if (placer != null) {
            placer.incrementStat("generators_placed", 1);
        }
        return new PlacementResult(true, "ok", state);
    }

    /** Removes a generator (block broken / island deleted). */
    public Optional<GeneratorState> remove(BlockPosition position) {
        GeneratorState state = byPosition.remove(position.key());
        if (state == null) {
            return Optional.empty();
        }
        Map<UUID, GeneratorState> island = byIsland.get(state.islandId());
        if (island != null) {
            island.remove(state.id());
        }
        return Optional.of(state);
    }

    public void removeIsland(UUID islandId) {
        for (GeneratorState state : ofIsland(islandId)) {
            remove(state.position());
        }
        byIsland.remove(islandId);
    }

    /** Validates an upgrade without charging. */
    public UpgradeOutcome previewUpgrade(IslandProfile island, PlayerProfile payer, GeneratorState state) {
        Optional<GeneratorType> maybeType = type(state.typeId());
        if (maybeType.isEmpty()) {
            return UpgradeOutcome.fail("unknown-type", state.tier());
        }
        GeneratorType type = maybeType.get();
        int next = state.tier() + 1;
        Optional<GeneratorTier> maybeTier = type.tier(next);
        if (maybeTier.isEmpty()) {
            return UpgradeOutcome.fail("max-tier", state.tier());
        }
        GeneratorTier tier = maybeTier.get();
        if (!state.islandId().equals(island.islandId())) {
            return UpgradeOutcome.fail("wrong-island", state.tier());
        }
        if (islands.levelOf(island) < tier.requiredIslandLevel()) {
            return UpgradeOutcome.fail("island-level", state.tier());
        }
        if (!tier.requiredUnlock().isEmpty() && !islands.isUnlocked(island, tier.requiredUnlock())) {
            return UpgradeOutcome.fail("locked", state.tier());
        }
        if (!tokens.has(payer, tier.tokenCost())) {
            return UpgradeOutcome.fail("insufficient-tokens", state.tier());
        }
        if (tier.moneyCost() > 0) {
            if (!economy.available()) {
                return UpgradeOutcome.fail("economy-unavailable", state.tier());
            }
            if (!economy.has(payer.playerId(), tier.moneyCost())) {
                return UpgradeOutcome.fail("insufficient-money", state.tier());
            }
        }
        return UpgradeOutcome.ok(next, tier.tokenCost(), tier.moneyCost());
    }

    /** Charges and applies a tier upgrade. */
    public UpgradeOutcome upgrade(IslandProfile island, PlayerProfile payer, GeneratorState state) {
        UpgradeOutcome preview = previewUpgrade(island, payer, state);
        if (!preview.success()) {
            return preview;
        }
        if (preview.tokensSpent() > 0 && !tokens.take(payer, preview.tokensSpent()).success()) {
            return UpgradeOutcome.fail("insufficient-tokens", state.tier());
        }
        if (preview.moneySpent() > 0 && !economy.withdraw(payer.playerId(), preview.moneySpent())) {
            if (preview.tokensSpent() > 0) {
                tokens.give(payer, preview.tokensSpent());
            }
            return UpgradeOutcome.fail("insufficient-money", state.tier());
        }
        state.tier(preview.newTier());
        island.markDirty();
        return preview;
    }

    /** One production cycle result (the Bukkit layer turns this into items). */
    public record Production(GeneratorState state, String material, int amount) {
    }

    /**
     * Ticks a generator. Returns the produced item when the interval elapsed.
     * Island and mastery XP are granted here so generators feed progression.
     */
    public Optional<Production> tick(GeneratorState state, IslandProfile island, long nowMillis,
                                     java.util.random.RandomGenerator random) {
        Optional<GeneratorTier> maybeTier = type(state.typeId()).flatMap(type -> type.tier(state.tier()));
        if (maybeTier.isEmpty()) {
            return Optional.empty();
        }
        GeneratorTier tier = maybeTier.get();
        long intervalMillis = tier.intervalTicks() * 50L;
        if (nowMillis - state.lastProduceMillis() < intervalMillis) {
            return Optional.empty();
        }
        state.lastProduceMillis(nowMillis);
        Optional<String> material = tier.outputs().pick(random);
        if (material.isEmpty()) {
            return Optional.empty();
        }
        if (island != null) {
            if (tier.islandXpPerCycle() > 0) {
                islands.addXp(island, tier.islandXpPerCycle());
            }
            if (tier.masteryXpPerCycle() > 0 && mastery.contains(masteryCategoryId)) {
                mastery.addXp(island, masteryCategoryId, tier.masteryXpPerCycle());
            }
        }
        return Optional.of(new Production(state, material.get(), tier.outputAmount()));
    }
}
