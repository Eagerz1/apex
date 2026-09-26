package com.apexmc.skyblock.paper.task;

import com.apexmc.skyblock.core.data.GeneratorState;
import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import com.apexmc.skyblock.paper.util.Items;
import java.util.Optional;
import java.util.random.RandomGenerator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

/**
 * Ticks every loaded generator on the main thread (entity/world access must be
 * synchronous). Generators whose island or world is not loaded are skipped.
 */
public final class GeneratorTask implements Runnable {

    private final SkyblockRuntime runtime;
    private final RandomGenerator random = new java.util.Random();

    public GeneratorTask(SkyblockRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (GeneratorState state : runtime.services().generators().all()) {
            Optional<IslandProfile> island = runtime.profiles().cachedIsland(state.islandId());
            if (island.isEmpty()) {
                continue;
            }
            World world = org.bukkit.Bukkit.getWorld(state.position().world());
            if (world == null) {
                continue;
            }
            runtime.services().generators().tick(state, island.get(), now, random).ifPresent(production -> {
                Material material = Items.material(production.material(), null);
                if (material == null) {
                    runtime.plugin().getLogger().warning("Generator '" + state.typeId()
                            + "' produces unknown material '" + production.material() + "'");
                    return;
                }
                Location location = new Location(world, state.position().x() + 0.5d,
                        state.position().y() + 1.0d, state.position().z() + 0.5d);
                world.dropItemNaturally(location, new ItemStack(material, production.amount()));
            });
        }
    }
}
