package com.apexmc.skyblock.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apexmc.skyblock.core.data.BlockPosition;
import com.apexmc.skyblock.core.data.GeneratorState;
import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.generators.GeneratorService;
import com.apexmc.skyblock.core.generators.UpgradeOutcome;
import com.apexmc.skyblock.core.service.ProgressionServices;
import com.apexmc.skyblock.testsupport.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeneratorServiceTest {

    private ProgressionServices services;
    private GeneratorService generators;
    private PlayerProfile player;
    private IslandProfile island;

    @BeforeEach
    void setUp() {
        services = Fixtures.services();
        generators = services.generators();
        player = Fixtures.player();
        island = Fixtures.island(player);
    }

    private BlockPosition pos(int x) {
        return new BlockPosition("world", x, 64, 0);
    }

    @Test
    void placingRegistersOwnershipAndPosition() {
        var result = generators.place(island, player, "cobble_gen", pos(1), 1);
        assertTrue(result.success(), result.reason());
        assertEquals(1, generators.ofIsland(island.islandId()).size());
        assertTrue(generators.at(pos(1)).isPresent());
        assertEquals(1, player.stat("generators_placed"));
    }

    @Test
    void occupiedPositionsCannotBeReused() {
        generators.place(island, player, "cobble_gen", pos(1), 1);
        assertEquals("occupied", generators.place(island, player, "cobble_gen", pos(1), 1).reason());
        assertEquals(1, generators.ofIsland(island.islandId()).size());
    }

    @Test
    void perIslandLimitIsEnforced() {
        for (int i = 0; i < 24; i++) {
            assertTrue(generators.place(island, player, "cobble_gen", pos(i), 1).success());
        }
        assertEquals("limit-reached", generators.place(island, player, "cobble_gen", pos(99), 1).reason());
    }

    @Test
    void placingALockedTierIsRejected() {
        assertEquals("island-level", generators.place(island, player, "cobble_gen", pos(1), 3).reason());
    }

    @Test
    void removingDeletesTheState() {
        generators.place(island, player, "cobble_gen", pos(1), 1);
        assertTrue(generators.remove(pos(1)).isPresent());
        assertTrue(generators.at(pos(1)).isEmpty());
        assertTrue(generators.remove(pos(1)).isEmpty());
        assertEquals(0, generators.ofIsland(island.islandId()).size());
    }

    @Test
    void upgradingRequiresIslandLevelAndTokens() {
        GeneratorState state = generators.place(island, player, "cobble_gen", pos(1), 1).state();
        assertEquals("island-level", generators.upgrade(island, player, state).reason());

        services.islands().setLevel(island, 5);
        assertEquals("insufficient-tokens", generators.upgrade(island, player, state).reason());

        services.tokens().give(player, 1_000);
        UpgradeOutcome upgraded = generators.upgrade(island, player, state);
        assertTrue(upgraded.success(), upgraded.reason());
        assertEquals(2, state.tier());
        assertEquals(250, services.tokens().balance(player));
    }

    @Test
    void upgradingStopsAtTheHighestTier() {
        GeneratorState state = generators.place(island, player, "cobble_gen", pos(1), 1).state();
        services.islands().setLevel(island, 30);
        services.tokens().give(player, 1_000_000);
        for (int tier = 2; tier <= 6; tier++) {
            assertTrue(generators.upgrade(island, player, state).success(), "tier " + tier);
        }
        assertEquals("max-tier", generators.upgrade(island, player, state).reason());
        assertEquals(6, state.tier());
    }

    @Test
    void generatorsFromAnotherIslandCannotBeUpgraded() {
        GeneratorState state = generators.place(island, player, "cobble_gen", pos(1), 1).state();
        IslandProfile other = Fixtures.island(Fixtures.player());
        services.islands().setLevel(other, 30);
        services.tokens().give(player, 100_000);
        assertEquals("wrong-island", generators.upgrade(other, player, state).reason());
    }

    @Test
    void tickProducesOnlyAfterTheInterval() {
        GeneratorState state = generators.place(island, player, "cobble_gen", pos(1), 1).state();
        var random = Fixtures.fixedRandom(0.0d);
        assertTrue(generators.tick(state, island, 10_000L, random).isPresent());
        assertTrue(generators.tick(state, island, 10_500L, random).isEmpty());
        assertTrue(generators.tick(state, island, 20_000L, random).isPresent());
    }

    @Test
    void tickGrantsIslandAndGeneratorMasteryXp() {
        GeneratorState state = generators.place(island, player, "cobble_gen", pos(1), 1).state();
        generators.tick(state, island, 10_000L, Fixtures.fixedRandom(0.0d));
        assertEquals(1, island.islandXp());
        assertEquals(1, island.masteryXp("generators"));
    }

    @Test
    void tiersDifferInOutputAndSpeed() {
        var type = generators.type("cobble_gen").orElseThrow();
        assertEquals(6, type.maxTier());
        assertTrue(type.tier(1).orElseThrow().intervalTicks() > type.tier(6).orElseThrow().intervalTicks());
        assertFalse(type.tier(6).orElseThrow().outputs().entries().isEmpty());
        assertTrue(type.tier(6).orElseThrow().outputs().chanceOf("DIAMOND") > 0);
        assertEquals(0.0d, type.tier(1).orElseThrow().outputs().chanceOf("DIAMOND"), 0.0001d);
    }

    @Test
    void persistenceReloadRestoresGenerators() {
        generators.place(island, player, "cobble_gen", pos(1), 1);
        generators.place(island, player, "cobble_gen", pos(2), 1);
        var snapshot = generators.all();
        GeneratorService fresh = new GeneratorService(services.config().generators(), services.tokens(),
                services.islands(), services.mastery(), services.economy());
        fresh.load(snapshot);
        assertEquals(2, fresh.ofIsland(island.islandId()).size());
        assertTrue(fresh.at(pos(2)).isPresent());
    }

    @Test
    void islandRemovalClearsEverything() {
        generators.place(island, player, "cobble_gen", pos(1), 1);
        generators.place(island, player, "cobble_gen", pos(2), 1);
        generators.removeIsland(island.islandId());
        assertEquals(0, generators.ofIsland(island.islandId()).size());
        assertTrue(generators.all().isEmpty());
    }
}
