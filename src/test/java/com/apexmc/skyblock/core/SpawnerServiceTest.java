package com.apexmc.skyblock.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apexmc.skyblock.core.data.BlockPosition;
import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.data.SpawnerState;
import com.apexmc.skyblock.core.service.ProgressionServices;
import com.apexmc.skyblock.core.spawners.SpawnerService;
import com.apexmc.skyblock.testsupport.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpawnerServiceTest {

    private ProgressionServices services;
    private SpawnerService spawners;
    private PlayerProfile player;
    private IslandProfile island;

    @BeforeEach
    void setUp() {
        services = Fixtures.services();
        spawners = services.spawners();
        player = Fixtures.player();
        island = Fixtures.island(player);
        services.islands().setLevel(island, 7);
    }

    private BlockPosition pos(int x) {
        return new BlockPosition("world", x, 70, 0);
    }

    @Test
    void placementRequiresTheIslandLevelUnlock() {
        IslandProfile fresh = Fixtures.island(Fixtures.player());
        assertEquals("island-level", spawners.place(fresh, player, "progression_spawner", pos(1), 1).reason());
        assertTrue(spawners.place(island, player, "progression_spawner", pos(1), 1).success());
    }

    @Test
    void placingOnTheSameBlockStacks() {
        spawners.place(island, player, "progression_spawner", pos(1), 1);
        var stacked = spawners.place(island, player, "progression_spawner", pos(1), 1);
        assertTrue(stacked.success());
        assertEquals("stacked", stacked.reason());
        assertEquals(2, stacked.state().stackSize());
        assertEquals(1, spawners.ofIsland(island.islandId()).size());
    }

    @Test
    void stackLimitIsEnforced() {
        for (int i = 0; i < 5; i++) {
            assertTrue(spawners.place(island, player, "progression_spawner", pos(1), 1).success());
        }
        assertEquals("stack-limit", spawners.place(island, player, "progression_spawner", pos(1), 1).reason());
    }

    @Test
    void mismatchedTiersCannotStack() {
        services.islands().setLevel(island, 16);
        spawners.place(island, player, "progression_spawner", pos(1), 1);
        assertEquals("stack-mismatch", spawners.place(island, player, "progression_spawner", pos(1), 2).reason());
    }

    @Test
    void perIslandLimitIsEnforced() {
        for (int i = 0; i < 8; i++) {
            assertTrue(spawners.place(island, player, "progression_spawner", pos(i), 1).success());
        }
        assertEquals("limit-reached", spawners.place(island, player, "progression_spawner", pos(50), 1).reason());
    }

    @Test
    void removingOneShrinksTheStackBeforeDeleting() {
        spawners.place(island, player, "progression_spawner", pos(1), 1);
        spawners.place(island, player, "progression_spawner", pos(1), 1);
        spawners.removeOne(pos(1));
        assertEquals(1, spawners.at(pos(1)).orElseThrow().stackSize());
        spawners.removeOne(pos(1));
        assertTrue(spawners.at(pos(1)).isEmpty());
        assertTrue(spawners.removeOne(pos(1)).isEmpty());
    }

    @Test
    void upgradeCostScalesWithStackSize() {
        services.islands().setLevel(island, 16);
        spawners.place(island, player, "progression_spawner", pos(1), 1);
        SpawnerState state = spawners.place(island, player, "progression_spawner", pos(1), 1).state();
        services.tokens().give(player, 100_000);
        var outcome = spawners.upgrade(island, player, state);
        assertTrue(outcome.success(), outcome.reason());
        assertEquals(10_000, outcome.tokensSpent(), "5000 tokens per spawner in a stack of two");
        assertEquals(2, state.tier());
    }

    @Test
    void upgradingIsBlockedWithoutTokens() {
        services.islands().setLevel(island, 16);
        SpawnerState state = spawners.place(island, player, "progression_spawner", pos(1), 1).state();
        assertEquals("insufficient-tokens", spawners.upgrade(island, player, state).reason());
        assertEquals(1, state.tier());
    }

    @Test
    void maxTierIsRespected() {
        services.islands().setLevel(island, 30);
        services.tokens().give(player, 1_000_000);
        SpawnerState state = spawners.place(island, player, "progression_spawner", pos(1), 1).state();
        for (int tier = 2; tier <= 4; tier++) {
            assertTrue(spawners.upgrade(island, player, state).success());
        }
        assertEquals("max-tier", spawners.upgrade(island, player, state).reason());
    }

    @Test
    void tickRespectsIntervalAndNearbyEntityBudget() {
        SpawnerState state = spawners.place(island, player, "progression_spawner", pos(1), 1).state();
        assertTrue(spawners.tick(state, 100_000L, 0).isPresent());
        assertTrue(spawners.tick(state, 100_100L, 0).isEmpty(), "interval not elapsed");
        assertTrue(spawners.tick(state, 200_000L, 99).isEmpty(), "entity budget exhausted");
    }

    @Test
    void stackedSpawnersSpawnMoreButStayWithinTheCap() {
        SpawnerState state = spawners.place(island, player, "progression_spawner", pos(1), 1).state();
        state.stackSize(5);
        var plan = spawners.tick(state, 500_000L, 0).orElseThrow();
        assertEquals("ZOMBIE", plan.entityType());
        assertEquals(5, plan.amount());
        state.lastSpawnMillis(0L);
        var limited = spawners.tick(state, 1_000_000L, 8).orElseThrow();
        assertEquals(2, limited.amount());
    }

    @Test
    void higherTiersGrantSlayerBonuses() {
        services.islands().setLevel(island, 30);
        services.tokens().give(player, 1_000_000);
        SpawnerState state = spawners.place(island, player, "progression_spawner", pos(1), 1).state();
        assertEquals(0.0d, spawners.slayerBonus(state), 0.0001d);
        spawners.upgrade(island, player, state);
        assertEquals(0.1d, spawners.slayerBonus(state), 0.0001d);
    }

    @Test
    void persistedSpawnersReloadWithTheirStack() {
        spawners.place(island, player, "progression_spawner", pos(1), 1);
        spawners.place(island, player, "progression_spawner", pos(1), 1);
        SpawnerService fresh = new SpawnerService(services.config().spawners(), services.tokens(),
                services.islands(), services.economy());
        fresh.load(spawners.all());
        assertEquals(2, fresh.at(pos(1)).orElseThrow().stackSize());
        assertFalse(fresh.ofIsland(island.islandId()).isEmpty());
    }
}
