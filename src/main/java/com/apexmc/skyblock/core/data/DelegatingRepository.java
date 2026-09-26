package com.apexmc.skyblock.core.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Base class that forwards every call to another repository. Useful for
 * decorators (metrics, read-only modes) and for tests that need to make a
 * single operation fail.
 */
public class DelegatingRepository implements ProgressionRepository {

    private final ProgressionRepository delegate;

    public DelegatingRepository(ProgressionRepository delegate) {
        this.delegate = delegate;
    }

    protected ProgressionRepository delegate() {
        return delegate;
    }

    @Override
    public void initialize() {
        delegate.initialize();
    }

    @Override
    public Optional<PlayerProfile> loadPlayer(UUID playerId) {
        return delegate.loadPlayer(playerId);
    }

    @Override
    public void savePlayer(PlayerProfile profile) {
        delegate.savePlayer(profile);
    }

    @Override
    public Optional<IslandProfile> loadIsland(UUID islandId) {
        return delegate.loadIsland(islandId);
    }

    @Override
    public Optional<IslandProfile> loadIslandByOwner(UUID ownerId) {
        return delegate.loadIslandByOwner(ownerId);
    }

    @Override
    public void saveIsland(IslandProfile island) {
        delegate.saveIsland(island);
    }

    @Override
    public List<GeneratorState> loadGenerators() {
        return delegate.loadGenerators();
    }

    @Override
    public void saveGenerators(Collection<GeneratorState> generators) {
        delegate.saveGenerators(generators);
    }

    @Override
    public void deleteGenerator(UUID generatorId) {
        delegate.deleteGenerator(generatorId);
    }

    @Override
    public List<SpawnerState> loadSpawners() {
        return delegate.loadSpawners();
    }

    @Override
    public void saveSpawners(Collection<SpawnerState> spawners) {
        delegate.saveSpawners(spawners);
    }

    @Override
    public void deleteSpawner(UUID spawnerId) {
        delegate.deleteSpawner(spawnerId);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
