package com.apexmc.skyblock.core.data;

import java.util.UUID;

/** Persisted state of a single placed generator. */
public final class GeneratorState {

    private final UUID id;
    private final UUID islandId;
    private final BlockPosition position;
    private final String typeId;
    private int tier;
    private long lastProduceMillis;

    public GeneratorState(UUID id, UUID islandId, BlockPosition position, String typeId, int tier, long lastProduceMillis) {
        this.id = id;
        this.islandId = islandId;
        this.position = position;
        this.typeId = typeId;
        this.tier = Math.max(1, tier);
        this.lastProduceMillis = lastProduceMillis;
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

    public long lastProduceMillis() {
        return lastProduceMillis;
    }

    public void lastProduceMillis(long millis) {
        this.lastProduceMillis = millis;
    }
}
