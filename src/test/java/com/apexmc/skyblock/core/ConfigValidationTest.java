package com.apexmc.skyblock.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.core.config.ConfigException;
import com.apexmc.skyblock.core.config.ProgressionConfig;
import com.apexmc.skyblock.testsupport.Fixtures;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigValidationTest {

    @Test
    void shippedDefaultConfigurationParses() {
        ProgressionConfig config = Fixtures.parsedDefaults();
        assertEquals(30, config.islandLevels().maxLevel());
        assertEquals(6, config.roles().all().size());
        assertEquals(6, config.omniTools().size());
        assertEquals(6, config.mastery().size());
        assertEquals(5, config.skills().all().size());
        assertEquals(4, config.companionRarities().size());
        assertFalse(config.report().hasErrors());
    }

    @Test
    void shippedOmniToolsExposeAtLeastFifteenNodes() {
        ProgressionConfig config = Fixtures.parsedDefaults();
        int nodes = config.omniTools().values().stream().mapToInt(tool -> tool.nodes().size()).sum();
        assertTrue(nodes >= 15, "expected at least 15 upgrade nodes but found " + nodes);
    }

    @Test
    void shippedRarityMaxLevelsMatchTheProductSpec() {
        ProgressionConfig config = Fixtures.parsedDefaults();
        assertEquals(50, config.companionRarities().get("common").maxLevel());
        assertEquals(70, config.companionRarities().get("rare").maxLevel());
        assertEquals(100, config.companionRarities().get("epic").maxLevel());
        assertEquals(200, config.companionRarities().get("mythic").maxLevel());
    }

    @Test
    void islandLevelsMustBeContiguous() {
        Map<String, Object> raw = Fixtures.defaultConfigMap();
        Fixtures.section(raw, "island", "levels").remove("5");
        ConfigException ex = assertThrows(ConfigException.class, () -> ProgressionConfig.parse(Cfg.root(raw)));
        assertTrue(ex.getMessage().contains("contiguous"), ex.getMessage());
    }

    @Test
    void unknownRoleOmniToolIsRejected() {
        Map<String, Object> raw = Fixtures.defaultConfigMap();
        Fixtures.section(raw, "roles-config", "roles", "miner").put("omnitool", "does_not_exist");
        ConfigException ex = assertThrows(ConfigException.class, () -> ProgressionConfig.parse(Cfg.root(raw)));
        assertTrue(ex.getMessage().contains("unknown omnitool"), ex.getMessage());
    }

    @Test
    void unknownRequiredUnlockIsRejected() {
        Map<String, Object> raw = Fixtures.defaultConfigMap();
        Fixtures.section(raw, "generators-config", "generators", "cobble_gen", "tiers", "2")
                .put("required-unlock", "not_a_real_unlock");
        ConfigException ex = assertThrows(ConfigException.class, () -> ProgressionConfig.parse(Cfg.root(raw)));
        assertTrue(ex.getMessage().contains("not_a_real_unlock"), ex.getMessage());
    }

    @Test
    void unknownMasteryCategoryOnSkillIsRejected() {
        Map<String, Object> raw = Fixtures.defaultConfigMap();
        Fixtures.section(raw, "skills", "mining").put("mastery-category", "alchemy");
        ConfigException ex = assertThrows(ConfigException.class, () -> ProgressionConfig.parse(Cfg.root(raw)));
        assertTrue(ex.getMessage().contains("unknown mastery category"), ex.getMessage());
    }

    @Test
    void invalidTokenChanceIsRejectedWithPath() {
        Map<String, Object> raw = Fixtures.defaultConfigMap();
        Fixtures.section(raw, "skills", "mining", "sources", "STONE").put("token-chance", 4.0d);
        ConfigException ex = assertThrows(ConfigException.class, () -> ProgressionConfig.parse(Cfg.root(raw)));
        assertTrue(ex.getMessage().contains("token-chance"), ex.getMessage());
    }

    @Test
    void spawnerIntervalGuardBlocksEntitySpam() {
        Map<String, Object> raw = Fixtures.defaultConfigMap();
        Fixtures.section(raw, "spawners-config", "spawners", "progression_spawner", "tiers", "1")
                .put("interval-ticks", 1);
        ConfigException ex = assertThrows(ConfigException.class, () -> ProgressionConfig.parse(Cfg.root(raw)));
        assertTrue(ex.getMessage().contains("interval-ticks"), ex.getMessage());
    }

    @Test
    void unknownSeasonResetTargetIsRejected() {
        Map<String, Object> raw = Fixtures.defaultConfigMap();
        Fixtures.section(raw, "season").put("reset", List.of("island_xp", "everything"));
        ConfigException ex = assertThrows(ConfigException.class, () -> ProgressionConfig.parse(Cfg.root(raw)));
        assertTrue(ex.getMessage().contains("unknown reset target"), ex.getMessage());
    }

    @Test
    void missingSectionsReportTheirPath() {
        Map<String, Object> raw = Fixtures.defaultConfigMap();
        raw.remove("mastery");
        ConfigException ex = assertThrows(ConfigException.class, () -> ProgressionConfig.parse(Cfg.root(raw)));
        assertTrue(ex.getMessage().contains("mastery"), ex.getMessage());
    }

    @Test
    void typedGettersReportUsefulErrors() {
        Cfg cfg = Cfg.root(Map.of("section", Map.of("number", "abc", "flag", "maybe")));
        Cfg section = cfg.section("section");
        assertTrue(assertThrows(ConfigException.class, () -> section.getLong("number")).getMessage().contains("whole number"));
        assertTrue(assertThrows(ConfigException.class, () -> section.getBoolean("flag", false)).getMessage().contains("true/false"));
        assertTrue(assertThrows(ConfigException.class, () -> section.section("number")).getMessage().contains("expected a section"));
    }

    @Test
    void messagesFallBackVisiblyWhenMissing() {
        ProgressionConfig config = Fixtures.parsedDefaults();
        assertTrue(config.messages().raw("does.not.exist").contains("missing message"));
        assertTrue(config.messages().has("island-level-up"));
        assertEquals("&aYou are now a &eMiner&a.", config.messages().get("role-selected", "role", "Miner"));
    }
}
