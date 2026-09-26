package com.apexmc.skyblock.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.omnitool.OmniToolService;
import com.apexmc.skyblock.core.omnitool.UpgradeResult;
import com.apexmc.skyblock.core.service.ProgressionServices;
import com.apexmc.skyblock.testsupport.FakeEconomy;
import com.apexmc.skyblock.testsupport.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OmniToolServiceTest {

    private ProgressionServices services;
    private OmniToolService tools;
    private PlayerProfile player;
    private IslandProfile island;
    private FakeEconomy economy;

    @BeforeEach
    void setUp() {
        economy = new FakeEconomy();
        services = Fixtures.services(economy);
        tools = services.omniTools();
        player = Fixtures.player();
        island = Fixtures.island(player);
    }

    private OmniToolService.UpgradeContext context(int roleLevel, int islandLevel) {
        services.islands().setLevel(island, islandLevel);
        return new OmniToolService.UpgradeContext(roleLevel, islandLevel,
                unlock -> services.islands().isUnlocked(island, unlock));
    }

    @Test
    void everyRoleHasItsOwnTool() {
        assertTrue(tools.toolForRole("miner").isPresent());
        assertTrue(tools.toolForRole("farmer").isPresent());
        assertTrue(tools.toolForRole("fisher").isPresent());
        assertTrue(tools.toolForRole("slayer").isPresent());
        assertTrue(tools.toolForRole("logger").isPresent());
        assertTrue(tools.toolForRole("nonexistent").isEmpty());
    }

    @Test
    void upgradingChargesTokensAndRaisesTheNodeLevel() {
        services.tokens().give(player, 10_000);
        UpgradeResult result = tools.upgrade(player, "omni_pickaxe", "efficiency", context(10, 10));
        assertTrue(result.success(), result.reason());
        assertEquals(1, result.newLevel());
        assertEquals(150, result.tokensSpent());
        assertEquals(9_850, services.tokens().balance(player));
        assertEquals(1, tools.nodeLevel(player, "omni_pickaxe", "efficiency"));
    }

    @Test
    void upgradeCostsScaleWithNodeLevel() {
        services.tokens().give(player, 100_000);
        var ctx = context(50, 30);
        tools.upgrade(player, "omni_pickaxe", "efficiency", ctx);
        UpgradeResult second = tools.upgrade(player, "omni_pickaxe", "efficiency", ctx);
        assertEquals(270, second.tokensSpent());
    }

    @Test
    void roleLevelRequirementsAreEnforced() {
        services.tokens().give(player, 100_000);
        assertEquals("role-level", tools.upgrade(player, "omni_pickaxe", "efficiency", context(1, 30)).reason());
        assertEquals(0, tools.nodeLevel(player, "omni_pickaxe", "efficiency"));
    }

    @Test
    void islandLevelRequirementsAreEnforced() {
        services.tokens().give(player, 100_000);
        assertEquals("island-level", tools.upgrade(player, "omni_pickaxe", "area_mining", context(50, 5)).reason());
    }

    @Test
    void lockedNodesCannotBeUpgradedWithoutTheUnlock() {
        services.tokens().give(player, 500_000);
        var ctx = new OmniToolService.UpgradeContext(50, 30, unlock -> false);
        assertEquals("locked", tools.upgrade(player, "omni_pickaxe", "area_mining", ctx).reason());
    }

    @Test
    void insufficientTokensBlockUpgrades() {
        assertEquals("insufficient-tokens", tools.upgrade(player, "omni_pickaxe", "efficiency", context(10, 10)).reason());
    }

    @Test
    void maxLevelIsRespected() {
        services.tokens().give(player, 1_000_000);
        var ctx = context(50, 30);
        for (int i = 0; i < 5; i++) {
            assertTrue(tools.upgrade(player, "omni_pickaxe", "efficiency", ctx).success());
        }
        assertEquals("max-level", tools.upgrade(player, "omni_pickaxe", "efficiency", ctx).reason());
        assertEquals(5, tools.nodeLevel(player, "omni_pickaxe", "efficiency"));
    }

    @Test
    void unknownToolsAndNodesAreRejected() {
        assertEquals("unknown-tool", tools.upgrade(player, "nope", "efficiency", context(10, 10)).reason());
        assertEquals("unknown-node", tools.upgrade(player, "omni_pickaxe", "nope", context(10, 10)).reason());
    }

    @Test
    void effectValuesScaleWithNodeLevel() {
        services.tokens().give(player, 1_000_000);
        var ctx = context(50, 30);
        assertEquals(0.0d, tools.effectByType(player, "omni_pickaxe", "xp_bonus"), 0.0001d);
        tools.upgrade(player, "omni_pickaxe", "xp_bonus", ctx);
        tools.upgrade(player, "omni_pickaxe", "xp_bonus", ctx);
        assertEquals(0.06d, tools.effectByType(player, "omni_pickaxe", "xp_bonus"), 0.0001d);
    }

    @Test
    void failedMoneyWithdrawalRefundsTokens() {
        var rawConfig = Fixtures.defaultConfigMap();
        Fixtures.section(rawConfig, "omnitools-config", "omnitools", "omni_pickaxe", "nodes", "efficiency")
                .put("money-cost-base", 500.0d);
        ProgressionServices withMoney = new ProgressionServices(
                com.apexmc.skyblock.core.config.ProgressionConfig.parse(
                        com.apexmc.skyblock.core.config.Cfg.root(rawConfig)), economy);
        PlayerProfile profile = Fixtures.player();
        IslandProfile profileIsland = Fixtures.island(profile);
        withMoney.islands().setLevel(profileIsland, 30);
        withMoney.tokens().give(profile, 5_000);
        economy.set(profile.playerId(), 10_000);
        economy.failWithdrawals(true);
        var ctx = new OmniToolService.UpgradeContext(50, 30, unlock -> true);
        UpgradeResult result = withMoney.omniTools().upgrade(profile, "omni_pickaxe", "efficiency", ctx);
        assertFalse(result.success());
        assertEquals(5_000, withMoney.tokens().balance(profile), "tokens must be refunded when money fails");
        assertEquals(0, withMoney.omniTools().nodeLevel(profile, "omni_pickaxe", "efficiency"));
    }

    @Test
    void moneyCostsAreBlockedWithoutVaultWhenConfigured() {
        var rawConfig = Fixtures.defaultConfigMap();
        Fixtures.section(rawConfig, "omnitools-config", "omnitools", "omni_pickaxe", "nodes", "efficiency")
                .put("money-cost-base", 500.0d);
        Fixtures.section(rawConfig, "economy").put("missing-vault-behaviour", "block");
        ProgressionServices noVault = new ProgressionServices(
                com.apexmc.skyblock.core.config.ProgressionConfig.parse(
                        com.apexmc.skyblock.core.config.Cfg.root(rawConfig)),
                new com.apexmc.skyblock.core.economy.DisabledEconomy());
        PlayerProfile profile = Fixtures.player();
        noVault.tokens().give(profile, 5_000);
        var ctx = new OmniToolService.UpgradeContext(50, 30, unlock -> true);
        assertEquals("economy-unavailable", noVault.omniTools().upgrade(profile, "omni_pickaxe", "efficiency", ctx).reason());
        assertEquals(5_000, noVault.tokens().balance(profile));
    }

    @Test
    void moneyCostsAreWaivedWithoutVaultByDefault() {
        var rawConfig = Fixtures.defaultConfigMap();
        Fixtures.section(rawConfig, "omnitools-config", "omnitools", "omni_pickaxe", "nodes", "efficiency")
                .put("money-cost-base", 500.0d);
        ProgressionServices noVault = new ProgressionServices(
                com.apexmc.skyblock.core.config.ProgressionConfig.parse(
                        com.apexmc.skyblock.core.config.Cfg.root(rawConfig)),
                new com.apexmc.skyblock.core.economy.DisabledEconomy());
        PlayerProfile profile = Fixtures.player();
        noVault.tokens().give(profile, 5_000);
        var ctx = new OmniToolService.UpgradeContext(50, 30, unlock -> true);
        var result = noVault.omniTools().upgrade(profile, "omni_pickaxe", "efficiency", ctx);
        assertTrue(result.success(), result.reason());
        assertEquals(0.0d, result.moneySpent(), 0.0001d);
        assertEquals(4_850, noVault.tokens().balance(profile));
    }

    @Test
    void sanitizeDropsUnknownNodesAndClampsLevels() {
        player.toolUpgrades("omni_pickaxe").put("efficiency", 99);
        player.toolUpgrades("omni_pickaxe").put("ghost_node", 4);
        player.toolUpgrades("ghost_tool").put("whatever", 2);
        tools.sanitize(player);
        assertEquals(5, tools.nodeLevel(player, "omni_pickaxe", "efficiency"));
        assertEquals(0, tools.nodeLevel(player, "omni_pickaxe", "ghost_node"));
        assertTrue(player.toolUpgrades("ghost_tool").isEmpty());
    }

    @Test
    void previewDoesNotChargeAnything() {
        services.tokens().give(player, 10_000);
        UpgradeResult preview = tools.preview(player, "omni_pickaxe", "efficiency", context(10, 10));
        assertTrue(preview.success());
        assertEquals(10_000, services.tokens().balance(player));
        assertEquals(0, tools.nodeLevel(player, "omni_pickaxe", "efficiency"));
    }
}
