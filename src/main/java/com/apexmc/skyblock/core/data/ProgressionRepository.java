package com.apexmc.skyblock.core.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Storage abstraction for every persisted entity. Implementations must be safe
 * to call from asynchronous threads; the plugin never touches them from the
 * main server thread.
 */
public interface ProgressionRepository extends AutoCloseable {

    void initialize();

    Optional<PlayerProfile> loadPlayer(UUID playerId);

    void savePlayer(PlayerProfile profile);

    Optional<IslandProfile> loadIsland(UUID islandId);

    Optional<IslandProfile> loadIslandByOwner(UUID ownerId);

    void saveIsland(IslandProfile island);

    List<GeneratorState> loadGenerators();

    void saveGenerators(Collection<GeneratorState> generators);

    void deleteGenerator(UUID generatorId);

    List<SpawnerState> loadSpawners();

    void saveSpawners(Collection<SpawnerState> spawners);

    void deleteSpawner(UUID spawnerId);

    @Override
    void close();
}
