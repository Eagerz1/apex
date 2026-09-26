package com.apexmc.skyblock.core.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository. Used by the automated test suite and as an emergency
 * fallback when the configured database cannot be reached, so the server keeps
 * running (progression is then simply not persisted).
 */
public final class InMemoryRepository implements ProgressionRepository {

    private final Map<UUID, Map<String, String>> players = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, String>> islands = new ConcurrentHashMap<>();
    private final Map<UUID, GeneratorState> generators = new ConcurrentHashMap<>();
    private final Map<UUID, SpawnerState> spawners = new ConcurrentHashMap<>();

    @Override
    public void initialize() {
        // Nothing to do.
    }

    @Override
    public Optional<PlayerProfile> loadPlayer(UUID playerId) {
        return Optional.ofNullable(players.get(playerId)).map(row -> ProfileCodec.decodePlayer(new LinkedHashMap<>(row)));
    }

    @Override
    public void savePlayer(PlayerProfile profile) {
        players.put(profile.playerId(), ProfileCodec.encodePlayer(profile));
    }

    @Override
    public Optional<IslandProfile> loadIsland(UUID islandId) {
        return Optional.ofNullable(islands.get(islandId)).map(row -> ProfileCodec.decodeIsland(new LinkedHashMap<>(row)));
    }

    @Override
    public Optional<IslandProfile> loadIslandByOwner(UUID ownerId) {
        return islands.values().stream()
                .filter(row -> ownerId.toString().equals(row.get("owner_id")))
                .findFirst()
                .map(row -> ProfileCodec.decodeIsland(new LinkedHashMap<>(row)));
    }

    @Override
    public void saveIsland(IslandProfile island) {
        islands.put(island.islandId(), ProfileCodec.encodeIsland(island));
    }

    @Override
    public List<GeneratorState> loadGenerators() {
        return new ArrayList<>(generators.values());
    }

    @Override
    public void saveGenerators(Collection<GeneratorState> states) {
        states.forEach(state -> generators.put(state.id(), state));
    }

    @Override
    public void deleteGenerator(UUID generatorId) {
        generators.remove(generatorId);
    }

    @Override
    public List<SpawnerState> loadSpawners() {
        return new ArrayList<>(spawners.values());
    }

    @Override
    public void saveSpawners(Collection<SpawnerState> states) {
        states.forEach(state -> spawners.put(state.id(), state));
    }

    @Override
    public void deleteSpawner(UUID spawnerId) {
        spawners.remove(spawnerId);
    }

    @Override
    public void close() {
        // Nothing to do.
    }

    public int playerCount() {
        return players.size();
    }

    public int islandCount() {
        return islands.size();
    }
}
