package com.apexmc.skyblock.paper.task;

import com.apexmc.skyblock.core.data.SpawnerState;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

/**
 * Ticks progression spawners on the main thread, honouring the per-tier nearby
 * entity budget so stacked spawners can never flood an island with mobs.
 */
public final class SpawnerTask implements Runnable {

    private static final double SCAN_RADIUS = 8.0d;

    private final SkyblockRuntime runtime;

    public SpawnerTask(SkyblockRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (SpawnerState state : runtime.services().spawners().all()) {
            World world = org.bukkit.Bukkit.getWorld(state.position().world());
            if (world == null) {
                continue;
            }
            Location location = new Location(world, state.position().x() + 0.5d,
                    state.position().y() + 1.0d, state.position().z() + 0.5d);
            int nearby = world.getNearbyEntities(location, SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS).size();
            runtime.services().spawners().tick(state, now, nearby).ifPresent(plan -> {
                EntityType type = entityType(plan.entityType());
                if (type == null) {
                    runtime.plugin().getLogger().warning("Spawner '" + state.typeId()
                            + "' is configured with unknown entity '" + plan.entityType() + "'");
                    return;
                }
                for (int i = 0; i < plan.amount(); i++) {
                    world.spawnEntity(location, type);
                }
            });
        }
    }

    private EntityType entityType(String key) {
        try {
            return EntityType.valueOf(key.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
