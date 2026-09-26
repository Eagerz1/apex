package com.apexmc.skyblock.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apexmc.skyblock.core.companions.CompanionService;
import com.apexmc.skyblock.core.data.CompanionInstance;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.service.ProgressionServices;
import com.apexmc.skyblock.testsupport.Fixtures;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompanionServiceTest {

    private ProgressionServices services;
    private CompanionService companions;
    private PlayerProfile player;

    @BeforeEach
    void setUp() {
        services = Fixtures.services();
        companions = services.companions();
        player = Fixtures.player();
    }

    private CompanionInstance grant(String id) {
        return companions.grant(player, id, null, 30).companion();
    }

    @Test
    void grantingRequiresTheIslandLevel() {
        assertEquals("island-level", companions.grant(player, "rock_golem", null, 0).reason());
        assertTrue(companions.grant(player, "rock_golem", null, 13).success());
    }

    @Test
    void firstCompanionBecomesActive() {
        CompanionInstance companion = grant("rock_golem");
        assertEquals(companion.instanceId(), player.activeCompanionId().orElseThrow());
    }

    @Test
    void unknownCompanionsAndRaritiesAreRejected() {
        assertEquals("unknown-companion", companions.grant(player, "dragon", null, 30).reason());
        assertEquals("unknown-rarity", companions.grant(player, "rock_golem", "godly", 30).reason());
    }

    @Test
    void companionLimitIsEnforced() {
        for (int i = 0; i < 9; i++) {
            assertTrue(companions.grant(player, "rock_golem", null, 30).success());
        }
        assertEquals("companion-limit", companions.grant(player, "rock_golem", null, 30).reason());
        assertEquals(9, player.companions().size());
    }

    @Test
    void rarityDefinesTheMaximumLevel() {
        CompanionInstance common = grant("rock_golem");
        companions.addXp(player, common.instanceId(), Long.MAX_VALUE);
        assertEquals(50, companions.level(common));

        CompanionInstance mythic = grant("token_drake");
        companions.addXp(player, mythic.instanceId(), Long.MAX_VALUE);
        assertEquals(200, companions.level(mythic));
    }

    @Test
    void abilityScalesWithLevelAndRarity() {
        CompanionInstance common = grant("rock_golem");
        double base = companions.abilityValue(common);
        companions.addXp(player, common.instanceId(), 100_000);
        assertTrue(companions.abilityValue(common) > base);
    }

    @Test
    void activeAbilityOnlyCountsMatchingAbilities() {
        CompanionInstance common = grant("rock_golem");
        companions.setActive(player, common.instanceId());
        assertTrue(companions.activeAbility(player, "mining_xp_bonus") > 0);
        assertEquals(0.0d, companions.activeAbility(player, "token_find"), 0.0001d);
    }

    @Test
    void mergingPromotesTheRarityAndConsumesTheSacrifice() {
        CompanionInstance target = grant("rock_golem");
        CompanionInstance sacrifice = grant("rock_golem");
        companions.addXp(player, target.instanceId(), 5_000);
        companions.addXp(player, sacrifice.instanceId(), 5_000);
        services.tokens().give(player, 10_000);

        var result = companions.merge(player, target.instanceId(), sacrifice.instanceId(), 0.5d);
        assertTrue(result.success(), result.reason());
        assertEquals("rare", target.rarityId());
        assertEquals(1, player.companions().size());
        assertEquals(5_000, target.xp());
        assertEquals(9_500, services.tokens().balance(player), "merge cost must be charged");
    }

    @Test
    void mergingRequiresMatchingTypeAndRarity() {
        CompanionInstance golem = grant("rock_golem");
        CompanionInstance sprite = grant("harvest_sprite");
        services.tokens().give(player, 10_000);
        assertEquals("type-mismatch", companions.merge(player, golem.instanceId(), sprite.instanceId(), 0.5d).reason());
        assertEquals(2, player.companions().size(), "a failed merge must never destroy a companion");
    }

    @Test
    void mergingTheSameCompanionWithItselfIsRejected() {
        CompanionInstance golem = grant("rock_golem");
        assertEquals("invalid-selection", companions.merge(player, golem.instanceId(), golem.instanceId(), 0.5d).reason());
        assertEquals(1, player.companions().size());
    }

    @Test
    void mergingIsRejectedWithoutTokens() {
        CompanionInstance target = grant("rock_golem");
        CompanionInstance sacrifice = grant("rock_golem");
        assertEquals("insufficient-tokens", companions.merge(player, target.instanceId(), sacrifice.instanceId(), 0.5d).reason());
        assertEquals(2, player.companions().size());
        assertEquals("common", target.rarityId());
    }

    @Test
    void mythicCompanionsCannotBeMergedFurther() {
        CompanionInstance target = grant("token_drake");
        CompanionInstance sacrifice = grant("token_drake");
        services.tokens().give(player, 1_000_000);
        assertEquals("max-rarity", companions.merge(player, target.instanceId(), sacrifice.instanceId(), 0.5d).reason());
        assertEquals(2, player.companions().size());
    }

    @Test
    void mergedXpIsCappedByTheNewRarityCurve() {
        CompanionInstance target = grant("rock_golem");
        CompanionInstance sacrifice = grant("rock_golem");
        companions.addXp(player, target.instanceId(), Long.MAX_VALUE);
        companions.addXp(player, sacrifice.instanceId(), Long.MAX_VALUE);
        services.tokens().give(player, 10_000);
        companions.merge(player, target.instanceId(), sacrifice.instanceId(), 1.0d);
        assertTrue(target.xp() <= services.config().companionRarities().get("rare").curve().totalXpCap());
    }

    @Test
    void unknownInstancesAreIgnoredForXp() {
        assertFalse(companions.addXp(player, UUID.randomUUID(), 1_000).changed());
        assertFalse(companions.setActive(player, UUID.randomUUID()));
    }

    @Test
    void sanitizeDropsCompanionsRemovedFromConfiguration() {
        grant("rock_golem");
        player.companions().add(new CompanionInstance(UUID.randomUUID(), "ghost_pet", "common", 100));
        assertEquals(1, companions.sanitize(player).size());
        assertEquals(1, player.companions().size());
    }
}
