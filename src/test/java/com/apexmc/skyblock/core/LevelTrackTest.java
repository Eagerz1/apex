package com.apexmc.skyblock.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apexmc.skyblock.core.progression.LevelTrack;
import com.apexmc.skyblock.core.progression.LevelUpResult;
import com.apexmc.skyblock.core.progression.XpCurve;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LevelTrackTest {

    private XpCurve curve;
    private LevelTrack track;

    @BeforeEach
    void setUp() {
        curve = XpCurve.table(List.of(100L, 200L, 300L));
        track = new LevelTrack(curve);
    }

    @Test
    void addingXpAdvancesLevels() {
        LevelUpResult result = track.addXp(350);
        assertTrue(result.leveledUp());
        assertEquals(0, result.previousLevel());
        assertEquals(2, result.newLevel());
        assertEquals(2, result.levelsGained());
        assertEquals(350, track.totalXp());
    }

    @Test
    void xpIsCappedAtMaxLevel() {
        track.addXp(Long.MAX_VALUE);
        assertEquals(3, track.level());
        assertTrue(track.maxed());
        assertEquals(curve.totalXpCap(), track.totalXp());
        LevelUpResult noop = track.addXp(1000);
        assertFalse(noop.changed());
        assertEquals(curve.totalXpCap(), track.totalXp());
    }

    @Test
    void negativeAndZeroGainsAreIgnored() {
        assertFalse(track.addXp(0).changed());
        assertFalse(track.addXp(-500).changed());
        assertEquals(0, track.totalXp());
    }

    @Test
    void xpToNextLevelCountsDown() {
        track.addXp(150);
        assertEquals(1, track.level());
        assertEquals(50, track.xpIntoLevel());
        assertEquals(150, track.xpToNextLevel());
        assertEquals(0.25d, track.progress(), 0.0001d);
    }

    @Test
    void setLevelJumpsToThreshold() {
        track.setLevel(2);
        assertEquals(2, track.level());
        assertEquals(300, track.totalXp());
        track.setLevel(99);
        assertEquals(3, track.level());
        track.setLevel(-5);
        assertEquals(0, track.level());
    }

    @Test
    void removingXpNeverGoesNegative() {
        track.addXp(250);
        track.removeXp(1000);
        assertEquals(0, track.totalXp());
        assertEquals(0, track.level());
    }

    @Test
    void constructorClampsStoredXp() {
        LevelTrack loaded = new LevelTrack(curve, Long.MAX_VALUE);
        assertEquals(curve.totalXpCap(), loaded.totalXp());
        LevelTrack negative = new LevelTrack(curve, -50);
        assertEquals(0, negative.totalXp());
    }
}
