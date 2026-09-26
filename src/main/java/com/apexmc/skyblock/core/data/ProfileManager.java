package com.apexmc.skyblock.core.data;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Caches player and island profiles and moves every database access onto the
 * supplied {@link Executor} (the plugin's async pool). The main server thread
 * only ever touches the in-memory cache.
 */
public final class ProfileManager {

    private final ProgressionRepository repository;
    private final Executor executor;
    private final Consumer<Throwable> errorHandler;
    private final String seasonId;

    private final ConcurrentHashMap<UUID, PlayerProfile> players = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, IslandProfile> islands = new ConcurrentHashMap<>();

    public ProfileManager(ProgressionRepository repository, Executor executor, String seasonId,
                          Consumer<Throwable> errorHandler) {
        this.repository = repository;
        this.executor = executor;
        this.seasonId = seasonId;
        this.errorHandler = errorHandler;
    }

    public ProgressionRepository repository() {
        return repository;
    }

    /** Cached profile, if the player is loaded. */
    public Optional<PlayerProfile> cachedPlayer(UUID playerId) {
        return Optional.ofNullable(players.get(playerId));
    }

    public Optional<IslandProfile> cachedIsland(UUID islandId) {
        return Optional.ofNullable(islands.get(islandId));
    }

    public Collection<PlayerProfile> loadedPlayers() {
        return List.copyOf(players.values());
    }

    public Collection<IslandProfile> loadedIslands() {
        return List.copyOf(islands.values());
    }

    /** Loads (or creates) a player profile asynchronously. */
    public CompletableFuture<PlayerProfile> loadPlayer(UUID playerId) {
        PlayerProfile cached = players.get(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return CompletableFuture.supplyAsync(() -> repository.loadPlayer(playerId)
                .orElseGet(() -> new PlayerProfile(playerId, seasonId)), executor)
                .thenApply(profile -> {
                    players.putIfAbsent(playerId, profile);
                    return players.get(playerId);
                })
                .exceptionally(error -> {
                    errorHandler.accept(error);
                    return players.computeIfAbsent(playerId, id -> new PlayerProfile(id, seasonId));
                });
    }

    /** Loads (or creates) the island profile owned by a player. */
    public CompletableFuture<IslandProfile> loadIslandForOwner(UUID ownerId) {
        return CompletableFuture.supplyAsync(() -> repository.loadIslandByOwner(ownerId)
                .orElseGet(() -> new IslandProfile(UUID.randomUUID(), ownerId, seasonId)), executor)
                .thenApply(this::cache)
                .exceptionally(error -> {
                    errorHandler.accept(error);
                    return cache(new IslandProfile(UUID.randomUUID(), ownerId, seasonId));
                });
    }

    public CompletableFuture<Optional<IslandProfile>> loadIsland(UUID islandId) {
        IslandProfile cached = islands.get(islandId);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }
        return CompletableFuture.supplyAsync(() -> repository.loadIsland(islandId), executor)
                .thenApply(loaded -> loaded.map(this::cache))
                .exceptionally(error -> {
                    errorHandler.accept(error);
                    return Optional.empty();
                });
    }

    private IslandProfile cache(IslandProfile island) {
        IslandProfile existing = islands.putIfAbsent(island.islandId(), island);
        return existing != null ? existing : island;
    }

    public void cachePlayer(PlayerProfile profile) {
        players.put(profile.playerId(), profile);
    }

    public void cacheIsland(IslandProfile island) {
        islands.put(island.islandId(), island);
    }

    /** Saves a single player asynchronously and clears its dirty flag. */
    public CompletableFuture<Void> savePlayer(PlayerProfile profile) {
        profile.clearDirty();
        return CompletableFuture.runAsync(() -> repository.savePlayer(profile), executor)
                .exceptionally(error -> {
                    profile.markDirty();
                    errorHandler.accept(error);
                    return null;
                });
    }

    public CompletableFuture<Void> saveIsland(IslandProfile island) {
        island.clearDirty();
        return CompletableFuture.runAsync(() -> repository.saveIsland(island), executor)
                .exceptionally(error -> {
                    island.markDirty();
                    errorHandler.accept(error);
                    return null;
                });
    }

    /** Autosave pass: persists only dirty profiles. */
    public CompletableFuture<Integer> saveDirty() {
        List<PlayerProfile> dirtyPlayers = players.values().stream().filter(PlayerProfile::dirty).toList();
        List<IslandProfile> dirtyIslands = islands.values().stream().filter(IslandProfile::dirty).toList();
        dirtyPlayers.forEach(PlayerProfile::clearDirty);
        dirtyIslands.forEach(IslandProfile::clearDirty);
        int count = dirtyPlayers.size() + dirtyIslands.size();
        return CompletableFuture.supplyAsync(() -> {
            dirtyPlayers.forEach(repository::savePlayer);
            dirtyIslands.forEach(repository::saveIsland);
            return count;
        }, executor).exceptionally(error -> {
            dirtyPlayers.forEach(PlayerProfile::markDirty);
            dirtyIslands.forEach(IslandProfile::markDirty);
            errorHandler.accept(error);
            return 0;
        });
    }

    /** Unloads a player after persisting it (called on quit). */
    public CompletableFuture<Void> unloadPlayer(UUID playerId) {
        PlayerProfile profile = players.remove(playerId);
        if (profile == null) {
            return CompletableFuture.completedFuture(null);
        }
        return savePlayer(profile);
    }

    /**
     * Synchronous flush used on shutdown: the server is stopping, so pending
     * state must be written before the plugin disables.
     */
    public int flushBlocking() {
        int saved = 0;
        for (PlayerProfile profile : players.values()) {
            try {
                repository.savePlayer(profile);
                profile.clearDirty();
                saved++;
            } catch (RuntimeException ex) {
                errorHandler.accept(ex);
            }
        }
        for (IslandProfile island : islands.values()) {
            try {
                repository.saveIsland(island);
                island.clearDirty();
                saved++;
            } catch (RuntimeException ex) {
                errorHandler.accept(ex);
            }
        }
        return saved;
    }
}
