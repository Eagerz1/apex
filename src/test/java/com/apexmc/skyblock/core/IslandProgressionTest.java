package com.apexmc.skyblock.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.progression.IslandProgressionService;
import com.apexmc.skyblock.core.progression.LevelUpResult;
import com.apexmc.skyblock.core.service.ProgressionServices;
import com.apexmc.skyblock.testsupport.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IslandProgressionTest {

    private ProgressionServices services;
    private IslandProgressionService islands;
    private IslandProfile island;

    @BeforeEach
    void setUp() {
        services = Fixtures.services();
        islands = services.islands();
        PlayerProfile owner = Fixtures.player();
        island = Fixtures.island(owner);
    }

    @Test
    void newIslandStartsAtLevelZero() {
        assertEquals(0, islands.levelOf(island));
        assertEquals(0, island.islandXp());
    }

    @Test
    void firstLevelRequiresTheConfiguredXp() {
        long needed = islands.config().level(1).orElseThrow().xpRequired();
        islands.addXp(island, needed - 1);
        assertEquals(0, islands.levelOf(island));
        LevelUpResult result = islands.addXp(island, 1);
        assertTrue(result.leveledUp());
        assertEquals(1, islands.levelOf(island));
    }

    @Test
    void xpSourcesAreConfigurable() {
        assertEquals(1.0d, islands.config().xpPerSource("block_mined"), 0.0001d);
        islands.addXpFromSource(island, "block_mined", 500);
        assertEquals(500, island.islandXp());
        islands.addXpFromSource(island, "unknown_source", 500);
        assertEquals(500, island.islandXp());
    }

    @Test
    void levelCapIsRespected() {
        islands.addXp(island, Long.MAX_VALUE);
        assertEquals(30, islands.levelOf(island));
        long capped = island.islandXp();
        islands.addXp(island, 1_000_000);
        assertEquals(capped, island.islandXp());
    }

    @Test
    void unlocksFollowTheLevelTable() {
        assertFalse(islands.isUnlocked(island, "generator_tier_2"));
        islands.setLevel(island, 5);
        assertTrue(islands.isUnlocked(island, "generator_tier_2"));
        assertTrue(islands.isUnlocked(island, "basic_generators"));
        assertFalse(islands.isUnlocked(island, "generator_tier_3"));
    }

    @Test
    void grantedUnlocksBypassTheLevelTable() {
        island.grantedUnlocks().add("generator_tier_6");
        assertTrue(islands.isUnlocked(island, "generator_tier_6"));
    }

    @Test
    void unknownUnlocksAreNeverGranted() {
        islands.setLevel(island, 30);
        assertFalse(islands.isUnlocked(island, "not_configured"));
    }

    @Test
    void unlocksBetweenListsEveryCrossedLevel() {
        var unlocks = islands.unlocksBetween(0, 3);
        assertEquals(3, unlocks.size());
        assertTrue(unlocks.contains("role_selection"));
    }

    @Test
    void seasonalMultiplierScalesGains() {
        islands.globalMultiplier(2.0d);
        islands.addXp(island, 100);
        assertEquals(200, island.islandXp());
        islands.globalMultiplier(0.0d);
        islands.addXp(island, 100);
        assertEquals(200, island.islandXp());
    }

    @Test
    void adminOperationsAdjustLevels() {
        islands.setLevel(island, 10);
        assertEquals(10, islands.levelOf(island));
        islands.removeXp(island, Long.MAX_VALUE);
        assertEquals(0, islands.levelOf(island));
        islands.setXp(island, -5);
        assertEquals(0, island.islandXp());
    }

    @Test
    void thirtyLevelsAreConfiguredWithRewards() {
        assertEquals(30, islands.config().levels().size());
        islands.config().levels().forEach(level -> {
            assertTrue(level.xpRequired() > 0);
            assertFalse(level.unlocks().isEmpty(), "level " + level.level() + " unlocks nothing");
        });
    }
}
