package com.apexmc.skyblock.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.service.ProgressionServices;
import com.apexmc.skyblock.core.skills.SkillActionResult;
import com.apexmc.skyblock.core.skills.SkillService;
import com.apexmc.skyblock.core.skills.SkillType;
import com.apexmc.skyblock.testsupport.Fixtures;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SkillServiceTest {

    private ProgressionServices services;
    private SkillService skills;
    private PlayerProfile player;
    private IslandProfile island;
    private RandomGenerator noLuck;

    @BeforeEach
    void setUp() {
        services = Fixtures.services();
        skills = services.skills();
        player = Fixtures.player();
        island = Fixtures.island(player);
        noLuck = Fixtures.fixedRandom(0.999d);
    }

    @Test
    void miningGrantsRoleIslandAndMasteryXp() {
        SkillActionResult result = skills.handle(player, island, SkillType.MINING, "STONE", 1, noLuck);
        assertTrue(result.granted());
        assertEquals(1, result.reward().roleXp());
        assertEquals(1, result.reward().islandXp());
        assertEquals(1, result.reward().masteryXp());
        assertEquals(1, player.roleXp("miner"));
        assertEquals(1, island.islandXp());
        assertEquals(1, island.masteryXp("mining"));
    }

    @Test
    void unconfiguredTargetsAreIgnored() {
        SkillActionResult result = skills.handle(player, island, SkillType.MINING, "DIRT", 1, noLuck);
        assertFalse(result.handled());
        assertEquals(0, island.islandXp());
    }

    @Test
    void islandLevelGatesHighTierTargets() {
        SkillActionResult result = skills.handle(player, island, SkillType.MINING, "DIAMOND_ORE", 1, noLuck);
        assertTrue(result.handled());
        assertFalse(result.granted());
        assertEquals("island-level", result.blockedReason());
        assertEquals(0, island.islandXp());

        services.islands().setLevel(island, 10);
        assertTrue(skills.handle(player, island, SkillType.MINING, "DIAMOND_ORE", 1, noLuck).granted());
    }

    @Test
    void unitsAreCappedToPreventExploits() {
        SkillActionResult result = skills.handle(player, island, SkillType.MINING, "STONE", 10_000, noLuck);
        assertEquals(SkillService.MAX_UNITS_PER_ACTION, result.reward().roleXp());
    }

    @Test
    void nonPositiveUnitsGrantNothing() {
        assertFalse(skills.handle(player, island, SkillType.MINING, "STONE", 0, noLuck).handled());
        assertFalse(skills.handle(player, island, SkillType.MINING, "STONE", -5, noLuck).handled());
        assertEquals(0, island.islandXp());
    }

    @Test
    void tokenFindProcsWhenLucky() {
        RandomGenerator lucky = Fixtures.fixedRandom(0.0d);
        SkillActionResult result = skills.handle(player, island, SkillType.MINING, "STONE", 1, lucky);
        assertTrue(result.reward().tokens() > 0);
        assertEquals(result.reward().tokens(), services.tokens().balance(player));
    }

    @Test
    void rareDropsRespectIslandLevelGates() {
        RandomGenerator lucky = Fixtures.fixedRandom(0.0d);
        SkillActionResult result = skills.handle(player, island, SkillType.MINING, "STONE", 1, lucky);
        assertTrue(result.drops().size() <= 1);
        result.firstDrop().ifPresent(drop -> assertEquals("enriched_coal", drop.id()));
    }

    @Test
    void masteryPassivesIncreaseXpGains() {
        services.mastery().setLevel(island, "mining", 25);
        SkillActionResult result = skills.handle(player, island, SkillType.MINING, "IRON_ORE", 1, noLuck);
        assertEquals(6, result.reward().roleXp(), "5 base XP with +20% mastery passives");
    }

    @Test
    void milestonesPayOutExactlyOnce() {
        player.stat(SkillService.statKey(SkillType.MINING), 999);
        SkillActionResult crossing = skills.handle(player, island, SkillType.MINING, "STONE", 1, noLuck);
        assertEquals(1, crossing.milestones().size());
        assertEquals(100, services.tokens().balance(player));

        SkillActionResult after = skills.handle(player, island, SkillType.MINING, "STONE", 1, noLuck);
        assertTrue(after.milestones().isEmpty());
        assertEquals(100, services.tokens().balance(player));
    }

    @Test
    void everySkillHasConfiguredSources() {
        for (SkillType type : SkillType.values()) {
            assertTrue(skills.registry().skill(type).isPresent(), type.id() + " missing");
            assertFalse(skills.registry().skill(type).orElseThrow().sources().isEmpty());
        }
    }

    @Test
    void fishingFeedsTheFisherRoleAndFishingMastery() {
        skills.handle(player, island, SkillType.FISHING, "COD", 1, noLuck);
        assertEquals(6, player.roleXp("fisher"));
        assertEquals(5, island.masteryXp("fishing"));
        assertEquals(0, player.roleXp("miner"));
    }

    @Test
    void slayerAndLoggingUseTheirOwnTracks() {
        skills.handle(player, island, SkillType.SLAYER, "ZOMBIE", 1, noLuck);
        skills.handle(player, island, SkillType.LOGGING, "OAK_LOG", 1, noLuck);
        assertEquals(5, player.roleXp("slayer"));
        assertEquals(3, player.roleXp("logger"));
        assertEquals(4, island.masteryXp("slayer"));
        assertEquals(3, island.masteryXp("logging"));
    }

    @Test
    void statisticsTrackEveryHandledAction() {
        skills.handle(player, island, SkillType.FARMING, "WHEAT", 5, noLuck);
        assertEquals(5, player.stat(SkillService.statKey(SkillType.FARMING)));
    }
}
