package com.apexmc.skyblock.paper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.paper.util.Text;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests for the platform layer helpers that do not need a running server. */
class PlatformSupportTest {

    @Test
    void identifiersAreHumanReadableInMessages() {
        assertEquals("Generator Tier 2", SkyblockRuntime.prettify("generator_tier_2"));
        assertEquals("Auto Replant", SkyblockRuntime.prettify("auto-replant"));
        assertEquals("", SkyblockRuntime.prettify(null));
    }

    @Test
    void progressBarsAreClampedToTheirLength() {
        assertEquals(20, Text.bar(0.5d, 20, "", "").length());
        assertEquals(20, Text.bar(5.0d, 20, "", "").length());
        assertEquals(20, Text.bar(-1.0d, 20, "", "").length());
        assertTrue(Text.percent(0.333d).startsWith("33.3"));
    }

    @Test
    void guiSectionFallsBackToSensibleDefaults() {
        Cfg gui = Cfg.root(Map.of("gui", Map.of("progression-title", "&8Custom"))).section("gui");
        assertEquals("&8Custom", gui.getString("progression-title", "&8Progression"));
        assertEquals("GRAY_STAINED_GLASS_PANE", gui.getString("filler-material", "GRAY_STAINED_GLASS_PANE"));
    }
}
