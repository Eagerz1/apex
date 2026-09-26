package com.apexmc.skyblock.core.data;

import java.util.UUID;

/** Persisted state of a single placed progression spawner. */
public final class SpawnerState {

    private final UUID id;
    private final UUID islandId;
    private final BlockPosition position;
    private final String typeId;
    private int tier;
    private int stackSize;
    private long lastSpawnMillis;

    public SpawnerState(UUID id, UUID islandId, BlockPosition position, String typeId, int tier, int stackSize, long lastSpawnMillis) {
        this.id = id;
        this.islandId = islandId;
        this.position = position;
        this.typeId = typeId;
        this.tier = Math.max(1, tier);
        this.stackSize = Math.max(1, stackSize);
        this.lastSpawnMillis = lastSpawnMillis;
    }

    public UUID id() {
        return id;
    }

    public UUID islandId() {
        return islandId;
    }

    public BlockPosition position() {
        return position;
    }

    public String typeId() {
        return typeId;
    }

    public int tier() {
        return tier;
    }

    public void tier(int tier) {
        this.tier = Math.max(1, tier);
    }

    public int stackSize() {
        return stackSize;
    }

    public void stackSize(int stackSize) {
        this.stackSize = Math.max(1, stackSize);
    }

    public long lastSpawnMillis() {
        return lastSpawnMillis;
    }

    public void lastSpawnMillis(long millis) {
        this.lastSpawnMillis = millis;
    }
}
