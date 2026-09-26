package com.apexmc.skyblock.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.core.config.ConfigException;
import com.apexmc.skyblock.core.progression.XpCurve;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class XpCurveTest {

    @Test
    void tableCurveResolvesCumulativeThresholds() {
        XpCurve curve = XpCurve.table(List.of(100L, 200L, 300L));
        assertEquals(3, curve.maxLevel());
        assertEquals(0, curve.cumulativeXp(0));
        assertEquals(100, curve.cumulativeXp(1));
        assertEquals(300, curve.cumulativeXp(2));
        assertEquals(600, curve.cumulativeXp(3));
        assertEquals(600, curve.totalXpCap());
    }

    @Test
    void levelForMatchesThresholdBoundaries() {
        XpCurve curve = XpCurve.table(List.of(100L, 200L, 300L));
        assertEquals(0, curve.levelFor(0));
        assertEquals(0, curve.levelFor(99));
        assertEquals(1, curve.levelFor(100));
        assertEquals(1, curve.levelFor(299));
        assertEquals(2, curve.levelFor(300));
        assertEquals(3, curve.levelFor(600));
        assertEquals(3, curve.levelFor(Long.MAX_VALUE));
    }

    @Test
    void progressInsideLevelIsReported() {
        XpCurve curve = XpCurve.table(List.of(100L, 200L));
        assertEquals(50, curve.progressInLevel(150));
        assertEquals(0.25d, curve.progressFraction(150), 0.0001d);
        assertEquals(1.0d, curve.progressFraction(300), 0.0001d);
        assertEquals(0, curve.progressInLevel(300));
    }

    @Test
    void linearCurveGrowsByStep() {
        XpCurve curve = XpCurve.linear(4, 100, 50);
        assertEquals(100, curve.xpForNextLevel(0));
        assertEquals(150, curve.xpForNextLevel(1));
        assertEquals(250, curve.xpForNextLevel(3));
        assertEquals(0, curve.xpForNextLevel(4));
    }

    @Test
    void geometricCurveGrowsByFactor() {
        XpCurve curve = XpCurve.geometric(3, 100, 2.0d);
        assertEquals(100, curve.xpForNextLevel(0));
        assertEquals(200, curve.xpForNextLevel(1));
        assertEquals(400, curve.xpForNextLevel(2));
        assertEquals(700, curve.totalXpCap());
    }

    @Test
    void invalidCurvesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> XpCurve.table(List.of()));
        assertThrows(IllegalArgumentException.class, () -> XpCurve.table(List.of(0L)));
        assertThrows(IllegalArgumentException.class, () -> XpCurve.geometric(5, 100, 0.5d));
        assertThrows(IllegalArgumentException.class, () -> XpCurve.linear(0, 100, 1));
    }

    @Test
    void curveIsBuiltFromConfiguration() {
        Cfg cfg = Cfg.root(Map.of("curve", Map.of("type", "linear", "max-level", 3, "base", 10, "step", 5)));
        XpCurve curve = XpCurve.fromConfig(cfg.section("curve"));
        assertEquals(3, curve.maxLevel());
        assertEquals(10, curve.xpForNextLevel(0));
        assertEquals(20, curve.xpForNextLevel(2));
    }

    @Test
    void unknownCurveTypeIsReportedWithPath() {
        Cfg cfg = Cfg.root(Map.of("curve", Map.of("type", "magic")));
        ConfigException ex = assertThrows(ConfigException.class, () -> XpCurve.fromConfig(cfg.section("curve")));
        assertTrue(ex.getMessage().contains("curve.type"), ex.getMessage());
    }
}
