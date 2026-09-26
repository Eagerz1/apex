package com.apexmc.skyblock.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.roles.RoleService;
import com.apexmc.skyblock.core.service.ProgressionServices;
import com.apexmc.skyblock.testsupport.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoleServiceTest {

    private ProgressionServices services;
    private RoleService roles;
    private PlayerProfile player;

    @BeforeEach
    void setUp() {
        services = Fixtures.services();
        roles = services.roles();
        player = Fixtures.player();
    }

    @Test
    void defaultRoleIsUsedWhenNothingSelected() {
        assertEquals("miner", roles.activeRoleId(player));
    }

    @Test
    void selectingARoleSucceeds() {
        RoleService.RoleChange change = roles.selectRole(player, "farmer", 0);
        assertTrue(change.success());
        assertEquals("farmer", roles.activeRoleId(player));
    }

    @Test
    void reselectingTheActiveRoleIsRejected() {
        roles.selectRole(player, "farmer", 0);
        assertEquals("already-active", roles.selectRole(player, "farmer", 0).reason());
    }

    @Test
    void unknownRolesAreRejected() {
        assertEquals("unknown-role", roles.selectRole(player, "wizard", 30).reason());
    }

    @Test
    void islandLevelGatesRoleSelection() {
        assertEquals("island-level-required", roles.selectRole(player, "universal", 0).reason());
        assertTrue(roles.selectRole(player, "universal", 12).success());
    }

    @Test
    void changingRolesPreservesProgression() {
        roles.addXp(player, "miner", 5_000);
        int minerLevel = roles.levelOf(player, "miner");
        assertTrue(minerLevel > 0);
        roles.selectRole(player, "fisher", 0);
        roles.addXp(player, "fisher", 400);
        assertEquals(minerLevel, roles.levelOf(player, "miner"));
        assertTrue(roles.levelOf(player, "fisher") >= 1);
        roles.selectRole(player, "miner", 0);
        assertEquals(minerLevel, roles.levelOf(player, "miner"));
    }

    @Test
    void roleXpIsCappedAtMaxLevel() {
        roles.addXp(player, "miner", Long.MAX_VALUE);
        assertEquals(50, roles.levelOf(player, "miner"));
        long stored = player.roleXp("miner");
        roles.addXp(player, "miner", 10_000);
        assertEquals(stored, player.roleXp("miner"));
    }

    @Test
    void perksApplyOnlyFromTheirLevel() {
        assertEquals(0.0d, roles.activePerkValue(player, "xp_bonus"), 0.0001d);
        roles.setLevel(player, "miner", 5);
        assertEquals(0.03d, roles.activePerkValue(player, "xp_bonus"), 0.0001d);
        roles.setLevel(player, "miner", 30);
        assertEquals(0.08d, roles.activePerkValue(player, "xp_bonus"), 0.0001d);
    }

    @Test
    void activeRoleXpGoesToTheSelectedRole() {
        roles.selectRole(player, "logger", 0);
        roles.addActiveXp(player, 1_000);
        assertTrue(player.roleXp("logger") > 0);
        assertEquals(0, player.roleXp("miner"));
    }

    @Test
    void adminXpOperationsAreClamped() {
        roles.setXp(player, "miner", -10);
        assertEquals(0, player.roleXp("miner"));
        roles.setLevel(player, "miner", 999);
        assertEquals(50, roles.levelOf(player, "miner"));
        roles.removeXp(player, "miner", Long.MAX_VALUE);
        assertEquals(0, roles.levelOf(player, "miner"));
    }

    @Test
    void negativeXpGainsAreIgnored() {
        assertFalse(roles.addXp(player, "miner", -50).changed());
        assertEquals(0, player.roleXp("miner"));
    }
}
