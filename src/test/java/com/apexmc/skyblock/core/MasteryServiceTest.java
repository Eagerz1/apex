package com.apexmc.skyblock.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.mastery.MasteryService;
import com.apexmc.skyblock.core.service.ProgressionServices;
import com.apexmc.skyblock.testsupport.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MasteryServiceTest {

    private ProgressionServices services;
    private MasteryService mastery;
    private IslandProfile island;

    @BeforeEach
    void setUp() {
        services = Fixtures.services();
        mastery = services.mastery();
        island = Fixtures.island(Fixtures.player());
    }

    @Test
    void masteryXpIsSeparateFromIslandXp() {
        mastery.addXp(island, "mining", 5_000);
        assertTrue(island.masteryXp("mining") > 0);
        assertEquals(0, island.islandXp());
        assertEquals(0, island.masteryXp("farming"));
    }

    @Test
    void categoriesAreConfigDriven() {
        assertEquals(6, mastery.categories().size());
        assertTrue(mastery.contains("generators"));
        assertFalse(mastery.contains("alchemy"));
    }

    @Test
    void unknownCategoriesThrowForWriteOperations() {
        assertThrows(IllegalArgumentException.class, () -> mastery.addXp(island, "alchemy", 10));
        assertEquals(0, mastery.levelOf(island, "alchemy"));
    }

    @Test
    void unlocksBecomeAvailableAtTheirLevel() {
        assertFalse(mastery.hasUnlock(island, "mining", "efficient_hands"));
        mastery.setLevel(island, "mining", 3);
        assertTrue(mastery.hasUnlock(island, "mining", "efficient_hands"));
        assertFalse(mastery.hasUnlock(island, "mining", "lucky_touch"));
        mastery.setLevel(island, "mining", 7);
        assertTrue(mastery.hasUnlock(island, "mining", "lucky_touch"));
    }

    @Test
    void passiveValuesAccumulate() {
        mastery.setLevel(island, "mining", 25);
        assertEquals(0.20d, mastery.passiveValue(island, "mining", "xp_bonus"), 0.0001d);
        assertEquals(0.01d, mastery.passiveValue(island, "mining", "token_find"), 0.0001d);
    }

    @Test
    void totalPassiveValueSumsAllCategories() {
        mastery.setLevel(island, "mining", 3);
        mastery.setLevel(island, "farming", 3);
        assertEquals(0.10d, mastery.totalPassiveValue(island, "xp_bonus"), 0.0001d);
    }

    @Test
    void masteryXpFromSourcesUsesConfiguredRates() {
        mastery.addXpFromSource(island, "mining", "block_mined", 250);
        assertEquals(250, island.masteryXp("mining"));
        mastery.addXpFromSource(island, "mining", "fish_caught", 250);
        assertEquals(250, island.masteryXp("mining"));
    }

    @Test
    void masteryIsCappedAtItsMaximumLevel() {
        mastery.addXp(island, "mining", Long.MAX_VALUE);
        assertEquals(25, mastery.levelOf(island, "mining"));
        long stored = island.masteryXp("mining");
        mastery.addXp(island, "mining", 1_000);
        assertEquals(stored, island.masteryXp("mining"));
    }

    @Test
    void unlockedListGrowsWithLevel() {
        mastery.setLevel(island, "fishing", 18);
        assertEquals(4, mastery.unlockedIn(island, "fishing").size());
    }
}
