package com.apexmc.skyblock.core.data;

/** World-agnostic block coordinate used by generator and spawner persistence. */
public record BlockPosition(String world, int x, int y, int z) {

    public BlockPosition {
        if (world == null || world.isBlank()) {
            throw new IllegalArgumentException("world must not be blank");
        }
    }

    public String key() {
        return world + ":" + x + ":" + y + ":" + z;
    }

    public static BlockPosition fromKey(String key) {
        String[] parts = key.split(":");
        if (parts.length != 4) {
            throw new IllegalArgumentException("malformed block key: " + key);
        }
        return new BlockPosition(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
    }
}
