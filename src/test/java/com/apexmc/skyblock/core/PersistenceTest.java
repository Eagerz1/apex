package com.apexmc.skyblock.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apexmc.skyblock.core.data.BlockPosition;
import com.apexmc.skyblock.core.data.CompanionInstance;
import com.apexmc.skyblock.core.data.GeneratorState;
import com.apexmc.skyblock.core.data.InMemoryRepository;
import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.data.ProfileCodec;
import com.apexmc.skyblock.core.data.ProfileManager;
import com.apexmc.skyblock.core.data.SpawnerState;
import com.apexmc.skyblock.core.data.sql.SqlDialect;
import com.apexmc.skyblock.core.util.Codec;
import com.apexmc.skyblock.testsupport.FailingRepository;
import com.apexmc.skyblock.testsupport.Fixtures;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PersistenceTest {

    private InMemoryRepository repository;
    private ProfileManager manager;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRepository();
        manager = new ProfileManager(repository, Runnable::run, "season-1", error -> {
            throw new IllegalStateException(error);
        });
    }

    @Test
    void codecRoundTripsEveryPlayerField() {
        PlayerProfile profile = Fixtures.player();
        profile.islandId(UUID.randomUUID());
        profile.activeRoleId("fisher");
        profile.tokens(12_345);
        profile.roleXp("miner", 500);
        profile.roleXp("fisher", 900);
        profile.stat("mining_count", 4_321);
        profile.toolUpgrades("omni_pickaxe").put("efficiency", 4);
        profile.toolUpgrades("omni_rod").put("fishing_luck", 2);
        CompanionInstance companion = new CompanionInstance(UUID.randomUUID(), "rock_golem", "rare", 777);
        profile.companions().add(companion);
        profile.activeCompanionId(companion.instanceId());

        PlayerProfile restored = ProfileCodec.decodePlayer(ProfileCodec.encodePlayer(profile));

        assertEquals(profile.playerId(), restored.playerId());
        assertEquals(profile.islandId(), restored.islandId());
        assertEquals("fisher", restored.activeRoleId().orElseThrow());
        assertEquals(12_345, restored.tokens());
        assertEquals(500, restored.roleXp("miner"));
        assertEquals(4_321, restored.stat("mining_count"));
        assertEquals(4, restored.toolUpgrades("omni_pickaxe").get("efficiency").intValue());
        assertEquals(2, restored.toolUpgrades("omni_rod").get("fishing_luck").intValue());
        assertEquals(1, restored.companions().size());
        assertEquals(777, restored.companions().get(0).xp());
        assertEquals(companion.instanceId(), restored.activeCompanionId().orElseThrow());
        assertFalse(restored.dirty(), "a freshly loaded profile must not be dirty");
    }

    @Test
    void codecRoundTripsIslands() {
        IslandProfile island = new IslandProfile(UUID.randomUUID(), UUID.randomUUID(), "season-2");
        island.islandXp(98_765);
        island.masteryXp("mining", 4_000);
        island.masteryXp("fishing", 250);
        island.grantedUnlocks().add("generator_tier_6");

        IslandProfile restored = ProfileCodec.decodeIsland(ProfileCodec.encodeIsland(island));
        assertEquals(island.islandId(), restored.islandId());
        assertEquals(island.ownerId(), restored.ownerId());
        assertEquals("season-2", restored.seasonId());
        assertEquals(98_765, restored.islandXp());
        assertEquals(4_000, restored.masteryXp("mining"));
        assertTrue(restored.grantedUnlocks().contains("generator_tier_6"));
    }

    @Test
    void codecSurvivesSeparatorCharactersInValues() {
        Map<String, String> encoded = Codec.decode(Codec.encode(Map.of("we;ird=key", "va=lue;with\\stuff")));
        assertEquals("va=lue;with\\stuff", encoded.get("we;ird=key"));
    }

    @Test
    void corruptStoredValuesDoNotBreakLoading() {
        PlayerProfile profile = Fixtures.player();
        Map<String, String> row = ProfileCodec.encodePlayer(profile);
        row.put("tokens", "not-a-number");
        row.put("companions", "garbage|data");
        row.put("active_companion", "not-a-uuid");
        row.put("tool_upgrades", "broken");
        PlayerProfile restored = ProfileCodec.decodePlayer(row);
        assertEquals(0, restored.tokens());
        assertTrue(restored.companions().isEmpty());
        assertTrue(restored.activeCompanionId().isEmpty());
    }

    @Test
    void repositoryPersistsAndReloadsProfiles() {
        PlayerProfile profile = Fixtures.player();
        profile.tokens(500);
        repository.savePlayer(profile);
        PlayerProfile loaded = repository.loadPlayer(profile.playerId()).orElseThrow();
        assertEquals(500, loaded.tokens());
        assertTrue(repository.loadPlayer(UUID.randomUUID()).isEmpty());
    }

    @Test
    void islandsCanBeLookedUpByOwner() {
        IslandProfile island = new IslandProfile(UUID.randomUUID(), UUID.randomUUID(), "season-1");
        repository.saveIsland(island);
        assertEquals(island.islandId(), repository.loadIslandByOwner(island.ownerId()).orElseThrow().islandId());
        assertTrue(repository.loadIslandByOwner(UUID.randomUUID()).isEmpty());
    }

    @Test
    void generatorAndSpawnerStatesPersist() {
        UUID islandId = UUID.randomUUID();
        GeneratorState generator = new GeneratorState(UUID.randomUUID(), islandId,
                new BlockPosition("world", 1, 2, 3), "cobble_gen", 3, 1_000L);
        SpawnerState spawner = new SpawnerState(UUID.randomUUID(), islandId,
                new BlockPosition("world", 4, 5, 6), "progression_spawner", 2, 3, 2_000L);
        repository.saveGenerators(List.of(generator));
        repository.saveSpawners(List.of(spawner));

        assertEquals(1, repository.loadGenerators().size());
        assertEquals(3, repository.loadGenerators().get(0).tier());
        assertEquals(3, repository.loadSpawners().get(0).stackSize());

        repository.deleteGenerator(generator.id());
        repository.deleteSpawner(spawner.id());
        assertTrue(repository.loadGenerators().isEmpty());
        assertTrue(repository.loadSpawners().isEmpty());
    }

    @Test
    void blockPositionKeysRoundTrip() {
        BlockPosition position = new BlockPosition("nether_world", -12, 64, 300);
        assertEquals(position, BlockPosition.fromKey(position.key()));
    }

    @Test
    void profileManagerCachesAndCreatesProfiles() {
        UUID playerId = UUID.randomUUID();
        PlayerProfile created = manager.loadPlayer(playerId).join();
        assertEquals(playerId, created.playerId());
        assertSameProfile(created, manager.loadPlayer(playerId).join());
        assertTrue(manager.cachedPlayer(playerId).isPresent());
    }

    private void assertSameProfile(PlayerProfile expected, PlayerProfile actual) {
        assertTrue(expected == actual, "profile manager must return the cached instance");
    }

    @Test
    void onlyDirtyProfilesAreSaved() {
        PlayerProfile profile = manager.loadPlayer(UUID.randomUUID()).join();
        assertEquals(0, manager.saveDirty().join().intValue());
        profile.tokens(100);
        assertEquals(1, manager.saveDirty().join().intValue());
        assertEquals(0, manager.saveDirty().join().intValue());
        assertEquals(100, repository.loadPlayer(profile.playerId()).orElseThrow().tokens());
    }

    @Test
    void unloadPersistsAndDropsFromTheCache() {
        PlayerProfile profile = manager.loadPlayer(UUID.randomUUID()).join();
        profile.tokens(42);
        manager.unloadPlayer(profile.playerId()).join();
        assertTrue(manager.cachedPlayer(profile.playerId()).isEmpty());
        assertEquals(42, repository.loadPlayer(profile.playerId()).orElseThrow().tokens());
    }

    @Test
    void shutdownFlushWritesEverything() {
        PlayerProfile profile = manager.loadPlayer(UUID.randomUUID()).join();
        profile.tokens(7);
        IslandProfile island = manager.loadIslandForOwner(profile.playerId()).join();
        island.islandXp(1_234);
        assertEquals(2, manager.flushBlocking());
        assertEquals(1_234, repository.loadIsland(island.islandId()).orElseThrow().islandXp());
    }

    @Test
    void storageFailuresKeepProfilesDirtyAndAreReported() {
        AtomicReference<Throwable> reported = new AtomicReference<>();
        FailingRepository failing = new FailingRepository();
        ProfileManager resilient = new ProfileManager(failing, Runnable::run, "season-1", reported::set);
        PlayerProfile profile = resilient.loadPlayer(UUID.randomUUID()).join();
        profile.tokens(10);
        resilient.savePlayer(profile).join();
        assertTrue(profile.dirty(), "failed saves must keep the profile dirty for a retry");
        assertTrue(reported.get() != null, "storage failures must be reported, never swallowed");
    }

    @Test
    void sqlSchemaAndUpsertsAreDialectAware() {
        assertEquals(6, SqlDialect.SQLITE.schema("apexsb_").size());
        assertTrue(SqlDialect.SQLITE.schema("apexsb_").get(0).contains("apexsb_players"));
        assertTrue(SqlDialect.SQLITE.upsert("apexsb_players", "player_id", List.of("player_id", "tokens"))
                .startsWith("INSERT OR REPLACE"));
        assertTrue(SqlDialect.MYSQL.upsert("apexsb_players", "player_id", List.of("player_id", "tokens"))
                .contains("ON DUPLICATE KEY UPDATE tokens=VALUES(tokens)"));
        assertEquals(SqlDialect.MYSQL, SqlDialect.fromId("mariadb"));
    }
}
