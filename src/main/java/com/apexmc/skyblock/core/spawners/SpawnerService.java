package com.apexmc.skyblock.core.spawners;

import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.core.config.ConfigException;
import com.apexmc.skyblock.core.currency.TokenService;
import com.apexmc.skyblock.core.data.BlockPosition;
import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.data.SpawnerState;
import com.apexmc.skyblock.core.economy.EconomyBridge;
import com.apexmc.skyblock.core.generators.UpgradeOutcome;
import com.apexmc.skyblock.core.progression.IslandProgressionService;
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
 * Progression aware spawners: tiered mobs, island level gates, stacking, hard
 * entity caps and safe persistence. Entity spam is prevented by a per-spawner
 * nearby-entity budget that the Bukkit layer supplies.
 */
public final class SpawnerService {

    private Map<String, SpawnerType> types;
    private final TokenService tokens;
    private final IslandProgressionService islands;
    private EconomyBridge economy;

    private final Map<String, SpawnerState> byPosition = new ConcurrentHashMap<>();
    private final Map<UUID, Map<UUID, SpawnerState>> byIsland = new ConcurrentHashMap<>();

    public SpawnerService(Map<String, SpawnerType> types, TokenService tokens,
                          IslandProgressionService islands, EconomyBridge economy) {
        this.types = Map.copyOf(types);
        this.tokens = tokens;
        this.islands = islands;
        this.economy = economy;
    }

    public static Map<String, SpawnerType> parse(Cfg cfg) {
        Cfg section = cfg.section("spawners");
        Map<String, SpawnerType> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, Cfg> entry : section.childSections().entrySet()) {
            String id = Ids.normalize(entry.getKey());
            Cfg spawner = entry.getValue();
            Cfg tiersSection = spawner.section("tiers");
            Map<Integer, Cfg> ordered = new java.util.TreeMap<>();
            for (Map.Entry<String, Cfg> tierEntry : tiersSection.childSections().entrySet()) {
                try {
                    ordered.put(Integer.parseInt(tierEntry.getKey().trim()), tierEntry.getValue());
                } catch (NumberFormatException ex) {
                    throw new ConfigException(tiersSection.path() + "." + tierEntry.getKey(), "tier keys must be whole numbers");
                }
            }
            if (ordered.isEmpty()) {
                throw new ConfigException(tiersSection.path(), "spawner '" + id + "' must define at least one tier");
            }
            List<SpawnerTier> tiers = new ArrayList<>();
            int expected = 1;
            for (Map.Entry<Integer, Cfg> tierEntry : ordered.entrySet()) {
                if (tierEntry.getKey() != expected) {
                    throw new ConfigException(tiersSection.path(),
                            "spawner tiers must be contiguous; expected " + expected + " but found " + tierEntry.getKey());
                }
                Cfg tier = tierEntry.getValue();
                int interval = tier.getInt("interval-ticks", 200);
                if (interval < 20) {
                    throw new ConfigException(tier.path() + ".interval-ticks",
                            "must be at least 20 ticks to avoid uncontrolled entity spam");
                }
                int spawnAmount = tier.getInt("spawn-amount", 1);
                if (spawnAmount < 1 || spawnAmount > 16) {
                    throw new ConfigException(tier.path() + ".spawn-amount", "must be between 1 and 16");
                }
                int maxNearby = tier.getInt("max-nearby-entities", 12);
                if (maxNearby < 1 || maxNearby > 64) {
                    throw new ConfigException(tier.path() + ".max-nearby-entities", "must be between 1 and 64");
                }
                tiers.add(new SpawnerTier(
                        expected,
                        tier.getString("display-name", id + " " + expected),
                        Ids.key(tier.getString("entity-type", "ZOMBIE")),
                        interval,
                        spawnAmount,
                        maxNearby,
                        Math.max(0, tier.getInt("required-island-level", 0)),
                        tier.getString("required-unlock", "").trim().toLowerCase(Locale.ROOT),
                        Math.max(0L, tier.getLong("token-cost", 0L)),
                        Math.max(0.0d, tier.getDouble("money-cost", 0.0d)),
                        Math.max(0.0d, tier.getDouble("slayer-xp-bonus", 0.0d)),
                        tier.getStringList("abilities")));
                expected++;
            }
            int maxStack = spawner.getInt("max-stack-size", 5);
            if (maxStack < 1 || maxStack > 100) {
                throw new ConfigException(spawner.path() + ".max-stack-size", "must be between 1 and 100");
            }
            parsed.put(id, new SpawnerType(
                    id,
                    spawner.getString("display-name", id),
                    Ids.key(spawner.getString("icon", "SPAWNER")),
                    Math.max(1, spawner.getInt("max-per-island", 8)),
                    maxStack,
                    tiers));
        }
        if (parsed.isEmpty()) {
            throw new ConfigException(section.path(), "at least one spawner type must be configured");
        }
        return parsed;
    }

    public void reload(Map<String, SpawnerType> types) {
        this.types = Map.copyOf(types);
    }

    public void economy(EconomyBridge economy) {
        this.economy = economy;
    }

    public Collection<SpawnerType> types() {
        return types.values();
    }

    public Optional<SpawnerType> type(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(types.get(id.toLowerCase(Locale.ROOT)));
    }

    public void load(Collection<SpawnerState> states) {
        byPosition.clear();
        byIsland.clear();
        states.forEach(this::index);
    }

    private void index(SpawnerState state) {
        byPosition.put(state.position().key(), state);
        byIsland.computeIfAbsent(state.islandId(), key -> new ConcurrentHashMap<>()).put(state.id(), state);
    }

    public Collection<SpawnerState> all() {
        return List.copyOf(byPosition.values());
    }

    public Collection<SpawnerState> ofIsland(UUID islandId) {
        return List.copyOf(byIsland.getOrDefault(islandId, Map.of()).values());
    }

    public Optional<SpawnerState> at(BlockPosition position) {
        return Optional.ofNullable(byPosition.get(position.key()));
    }

    public int countOfType(UUID islandId, String typeId) {
        return (int) ofIsland(islandId).stream().filter(state -> state.typeId().equals(typeId)).count();
    }

    /** Result of a placement attempt. */
    public record PlacementResult(boolean success, String reason, SpawnerState state) {
        public static PlacementResult fail(String reason) {
            return new PlacementResult(false, reason, null);
        }
    }

    /** Places a spawner, or stacks onto an existing identical spawner. */
    public PlacementResult place(IslandProfile island, PlayerProfile placer, String typeId, BlockPosition position, int tier) {
        Optional<SpawnerType> maybeType = type(typeId);
        if (maybeType.isEmpty()) {
            return PlacementResult.fail("unknown-type");
        }
        SpawnerType type = maybeType.get();
        Optional<SpawnerTier> maybeTier = type.tier(tier);
        if (maybeTier.isEmpty()) {
            return PlacementResult.fail("unknown-tier");
        }
        SpawnerTier definition = maybeTier.get();
        if (islands.levelOf(island) < definition.requiredIslandLevel()) {
            return PlacementResult.fail("island-level");
        }
        if (!definition.requiredUnlock().isEmpty() && !islands.isUnlocked(island, definition.requiredUnlock())) {
            return PlacementResult.fail("locked");
        }
        SpawnerState existing = byPosition.get(position.key());
        if (existing != null) {
            if (!existing.typeId().equals(type.id()) || existing.tier() != tier) {
                return PlacementResult.fail("stack-mismatch");
            }
            if (existing.stackSize() >= type.maxStackSize()) {
                return PlacementResult.fail("stack-limit");
            }
            existing.stackSize(existing.stackSize() + 1);
            return new PlacementResult(true, "stacked", existing);
        }
        if (countOfType(island.islandId(), type.id()) >= type.maxPerIsland()) {
            return PlacementResult.fail("limit-reached");
        }
        SpawnerState state = new SpawnerState(UUID.randomUUID(), island.islandId(), position, type.id(), tier, 1, 0L);
        index(state);
        if (placer != null) {
            placer.incrementStat("spawners_placed", 1);
        }
        return new PlacementResult(true, "ok", state);
    }

    /** Removes one spawner from a stack; deletes the state when the stack empties. */
    public Optional<SpawnerState> removeOne(BlockPosition position) {
        SpawnerState state = byPosition.get(position.key());
        if (state == null) {
            return Optional.empty();
        }
        if (state.stackSize() > 1) {
            state.stackSize(state.stackSize() - 1);
            return Optional.of(state);
        }
        byPosition.remove(position.key());
        Map<UUID, SpawnerState> island = byIsland.get(state.islandId());
        if (island != null) {
            island.remove(state.id());
        }
        return Optional.of(state);
    }

    public void removeIsland(UUID islandId) {
        for (SpawnerState state : ofIsland(islandId)) {
            byPosition.remove(state.position().key());
        }
        byIsland.remove(islandId);
    }

    public UpgradeOutcome previewUpgrade(IslandProfile island, PlayerProfile payer, SpawnerState state) {
        Optional<SpawnerType> maybeType = type(state.typeId());
        if (maybeType.isEmpty()) {
            return UpgradeOutcome.fail("unknown-type", state.tier());
        }
        SpawnerType type = maybeType.get();
        int next = state.tier() + 1;
        Optional<SpawnerTier> maybeTier = type.tier(next);
        if (maybeTier.isEmpty()) {
            return UpgradeOutcome.fail("max-tier", state.tier());
        }
        SpawnerTier tier = maybeTier.get();
        if (!state.islandId().equals(island.islandId())) {
            return UpgradeOutcome.fail("wrong-island", state.tier());
        }
        if (islands.levelOf(island) < tier.requiredIslandLevel()) {
            return UpgradeOutcome.fail("island-level", state.tier());
        }
        if (!tier.requiredUnlock().isEmpty() && !islands.isUnlocked(island, tier.requiredUnlock())) {
            return UpgradeOutcome.fail("locked", state.tier());
        }
        // Stacked spawners pay per spawner in the stack: no discount exploit.
        long tokenCost = tier.tokenCost() * state.stackSize();
        double moneyCost = tier.moneyCost() * state.stackSize();
        if (!tokens.has(payer, tokenCost)) {
            return UpgradeOutcome.fail("insufficient-tokens", state.tier());
        }
        if (moneyCost > 0) {
            if (!economy.available()) {
                return UpgradeOutcome.fail("economy-unavailable", state.tier());
            }
            if (!economy.has(payer.playerId(), moneyCost)) {
                return UpgradeOutcome.fail("insufficient-money", state.tier());
            }
        }
        return UpgradeOutcome.ok(next, tokenCost, moneyCost);
    }

    public UpgradeOutcome upgrade(IslandProfile island, PlayerProfile payer, SpawnerState state) {
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

    /** Planned spawn batch; the Bukkit layer performs the actual entity spawning. */
    public record SpawnPlan(SpawnerState state, String entityType, int amount) {
    }

    /**
     * Decides whether a spawner should spawn right now.
     *
     * @param nearbyEntities living entities currently near the spawner
     */
    public Optional<SpawnPlan> tick(SpawnerState state, long nowMillis, int nearbyEntities) {
        Optional<SpawnerTier> maybeTier = type(state.typeId()).flatMap(type -> type.tier(state.tier()));
        if (maybeTier.isEmpty()) {
            return Optional.empty();
        }
        SpawnerTier tier = maybeTier.get();
        if (nowMillis - state.lastSpawnMillis() < tier.intervalTicks() * 50L) {
            return Optional.empty();
        }
        state.lastSpawnMillis(nowMillis);
        int budget = tier.maxNearbyEntities() - Math.max(0, nearbyEntities);
        if (budget <= 0) {
            return Optional.empty();
        }
        int amount = Math.min(budget, tier.spawnAmount() * state.stackSize());
        if (amount <= 0) {
            return Optional.empty();
        }
        return Optional.of(new SpawnPlan(state, tier.entityType(), amount));
    }

    /** Extra slayer XP multiplier granted by the tier of the spawner that produced a mob. */
    public double slayerBonus(SpawnerState state) {
        return type(state.typeId()).flatMap(type -> type.tier(state.tier()))
                .map(SpawnerTier::slayerXpBonus).orElse(0.0d);
    }
}
