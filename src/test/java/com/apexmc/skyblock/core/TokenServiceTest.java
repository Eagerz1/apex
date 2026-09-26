package com.apexmc.skyblock.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apexmc.skyblock.core.currency.TokenService;
import com.apexmc.skyblock.core.currency.TokenTransaction;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.testsupport.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

    private TokenService tokens;
    private PlayerProfile player;

    @BeforeEach
    void setUp() {
        tokens = new TokenService("Sky Token", "Sky Tokens", "*", 1_000L);
        player = Fixtures.player();
    }

    @Test
    void givingTokensIncreasesTheBalance() {
        TokenTransaction result = tokens.give(player, 250);
        assertTrue(result.success());
        assertEquals(250, result.newBalance());
        assertEquals(250, tokens.balance(player));
    }

    @Test
    void nonPositiveAmountsAreRejected() {
        assertFalse(tokens.give(player, 0).success());
        assertFalse(tokens.give(player, -100).success());
        assertFalse(tokens.take(player, -100).success());
        assertEquals(0, tokens.balance(player));
    }

    @Test
    void balanceIsCappedAtTheConfiguredMaximum() {
        tokens.give(player, 900);
        TokenTransaction capped = tokens.give(player, 500);
        assertTrue(capped.success());
        assertEquals(1_000, tokens.balance(player));
        assertEquals(100, capped.delta());
    }

    @Test
    void takingMoreThanTheBalanceFails() {
        tokens.give(player, 100);
        TokenTransaction result = tokens.take(player, 500);
        assertFalse(result.success());
        assertEquals("insufficient-funds", result.reason());
        assertEquals(100, tokens.balance(player));
    }

    @Test
    void setNeverGoesNegativeAndRespectsTheCap() {
        assertFalse(tokens.set(player, -5).success());
        tokens.set(player, 5_000);
        assertEquals(1_000, tokens.balance(player));
    }

    @Test
    void transferMovesTokensWithoutCreatingThem() {
        PlayerProfile other = Fixtures.player();
        tokens.give(player, 400);
        assertTrue(tokens.transfer(player, other, 150).success());
        assertEquals(250, tokens.balance(player));
        assertEquals(150, tokens.balance(other));
    }

    @Test
    void transferIsRejectedWhenTheReceiverWouldExceedTheCap() {
        PlayerProfile other = Fixtures.player();
        tokens.give(player, 1_000);
        tokens.give(other, 1_000);
        assertFalse(tokens.transfer(player, other, 500).success());
        assertEquals(1_000, tokens.balance(player));
        assertEquals(1_000, tokens.balance(other));
    }

    @Test
    void selfTransferIsRejected() {
        tokens.give(player, 100);
        assertEquals("same-player", tokens.transfer(player, player, 50).reason());
        assertEquals(100, tokens.balance(player));
    }

    @Test
    void currencyCanBeRenamedEntirely() {
        tokens.rename("Shard", "Shards", "#", 10);
        assertEquals("Shards", tokens.displayNamePlural());
        assertEquals("#5 Shards", tokens.format(5));
        assertEquals("#1 Shard", tokens.format(1));
    }

    @Test
    void hasChecksAreLenientForZeroCosts() {
        assertTrue(tokens.has(player, 0));
        assertFalse(tokens.has(player, 1));
    }
}
