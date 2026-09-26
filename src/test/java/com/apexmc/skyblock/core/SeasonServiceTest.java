package com.apexmc.skyblock.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.core.config.ProgressionConfig;
import com.apexmc.skyblock.core.data.CompanionInstance;
import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.season.SeasonService;
import com.apexmc.skyblock.core.service.ProgressionServices;
import com.apexmc.skyblock.testsupport.Fixtures;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SeasonServiceTest {

    private ProgressionServices services;
    private PlayerProfile player;
    private IslandProfile island;

    @BeforeEach
    void setUp() {
        services = Fixtures.services();
        player = Fixtures.player();
        island = Fixtures.island(player);
    }

    private ProgressionServices withSeason(String seasonId, List<String> resets, boolean keepTokens, boolean keepCompanions) {
        Map<String, Object> raw = Fixtures.defaultConfigMap();
        Map<String, Object> season = Fixtures.section(raw, "season");
        season.put("season-id", seasonId);
        season.put("reset", resets);
        season.put("keep-tokens", keepTokens);
        season.put("keep-companions", keepCompanions);
        return new ProgressionServices(ProgressionConfig.parse(Cfg.root(raw)),
                new com.apexmc.skyblock.core.economy.DisabledEconomy());
    }

    @Test
    void currentSeasonProfilesAreNotReset() {
        SeasonService seasons = services.seasons();
        services.roles().addXp(player, "miner", 5_000);
        assertFalse(seasons.needsReset(player));
        assertFalse(seasons.applyTo(player).changedAnything());
        assertTrue(player.roleXp("miner") > 0);
    }

    @Test
    void rolloverResetsConfiguredTracks() {
        services.roles().addXp(player, "miner", 5_000);
        services.islands().addXp(island, 50_000);
        services.mastery().addXp(island, "mining", 5_000);
        player.toolUpgrades("omni_pickaxe").put("efficiency", 3);
        services.tokens().give(player, 1_000);

        ProgressionServices next = withSeason("season-2",
                List.of("island_xp", "mastery_xp", "role_xp", "omnitool_upgrades"), false, true);
        next.seasons().applyTo(player);
        next.seasons().applyTo(island);

        assertEquals(0, player.roleXp("miner"));
        assertEquals(0, island.islandXp());
        assertEquals(0, island.masteryXp("mining"));
        assertTrue(player.toolUpgrades("omni_pickaxe").isEmpty());
        assertEquals(0, player.tokens(), "tokens reset because keep-tokens is false");
        assertEquals("season-2", player.seasonId());
        assertEquals("season-2", island.seasonId());
    }

    @Test
    void protectedContentSurvivesTheReset() {
        services.companions().grant(player, "rock_golem", null, 30);
        services.tokens().give(player, 2_500);
        player.stat("lifetime_tokens_earned", 9_999);
        player.stat("mining_count", 4_321);
        island.grantedUnlocks().add("generator_tier_6");

        ProgressionServices next = withSeason("season-3", List.of("island_xp", "stats"), true, true);
        next.seasons().applyTo(player);
        next.seasons().applyTo(island);

        assertEquals(1, player.companions().size(), "purchased companions must survive");
        assertEquals(2_500, player.tokens(), "keep-tokens must be honoured");
        assertEquals(9_999, player.stat("lifetime_tokens_earned"), "protected stats must survive");
        assertEquals(0, player.stat("mining_count"));
        assertTrue(island.grantedUnlocks().contains("generator_tier_6"), "unlocks not listed are preserved");
    }

    @Test
    void resetIsIdempotentWithinASeason() {
        ProgressionServices next = withSeason("season-4", List.of("island_xp"), true, true);
        services.islands().addXp(island, 10_000);
        next.seasons().applyTo(island);
        assertEquals(0, island.islandXp());
        next.islands().addXp(island, 7_500);
        assertFalse(next.seasons().applyTo(island).changedAnything());
        assertEquals(7_500, island.islandXp(), "a second pass in the same season must not wipe progress");
    }

    @Test
    void companionsCanBeWipedWhenExplicitlyConfigured() {
        services.companions().grant(player, "rock_golem", null, 30);
        ProgressionServices next = withSeason("season-5", List.of(), false, false);
        next.seasons().applyTo(player);
        assertTrue(player.companions().isEmpty());
        assertTrue(player.activeCompanionId().isEmpty());
    }

    @Test
    void seasonMultipliersScaleEveryXpTrack() {
        Map<String, Object> raw = Fixtures.defaultConfigMap();
        Fixtures.section(raw, "season").put("xp-multiplier", 2.0d);
        ProgressionServices doubled = new ProgressionServices(ProgressionConfig.parse(Cfg.root(raw)),
                new com.apexmc.skyblock.core.economy.DisabledEconomy());
        doubled.islands().addXp(island, 100);
        doubled.roles().addXp(player, "miner", 100);
        assertEquals(200, island.islandXp());
        assertEquals(200, player.roleXp("miner"));
    }

    @Test
    void generatorAndSpawnerResetFlagsAreExposed() {
        assertTrue(services.seasons().resetsGenerators());
        assertTrue(services.seasons().resetsSpawners());
        ProgressionServices keep = withSeason("season-6", List.of("island_xp"), true, true);
        assertFalse(keep.seasons().resetsGenerators());
        assertFalse(keep.seasons().resetsSpawners());
    }

    @Test
    void islandsFromAnotherSeasonAreDetected() {
        IslandProfile old = new IslandProfile(UUID.randomUUID(), player.playerId(), "season-0");
        assertTrue(services.seasons().needsReset(old));
        PlayerProfile oldPlayer = new PlayerProfile(UUID.randomUUID(), "season-0");
        oldPlayer.companions().add(new CompanionInstance(UUID.randomUUID(), "rock_golem", "common", 10));
        assertTrue(services.seasons().needsReset(oldPlayer));
    }
}
