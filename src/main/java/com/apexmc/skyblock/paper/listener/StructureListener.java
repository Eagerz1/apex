package com.apexmc.skyblock.paper.listener;

import com.apexmc.skyblock.core.data.BlockPosition;
import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.generators.GeneratorService;
import com.apexmc.skyblock.core.generators.GeneratorType;
import com.apexmc.skyblock.core.spawners.SpawnerService;
import com.apexmc.skyblock.core.spawners.SpawnerType;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import java.util.Optional;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Registers and removes generators and spawners as their blocks are placed and
 * broken. Because state is keyed by block position, breaking and re-placing a
 * block can never duplicate a generator or a spawner stack.
 */
public final class StructureListener implements Listener {

    private final SkyblockRuntime runtime;

    public StructureListener(SkyblockRuntime runtime) {
        this.runtime = runtime;
    }

    private BlockPosition position(Block block) {
        return new BlockPosition(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Optional<PlayerProfile> maybeProfile = runtime.profile(player);
        if (maybeProfile.isEmpty()) {
            return;
        }
        PlayerProfile profile = maybeProfile.get();
        Optional<IslandProfile> maybeIsland = runtime.island(profile);
        if (maybeIsland.isEmpty()) {
            return;
        }
        IslandProfile island = maybeIsland.get();
        String material = event.getBlockPlaced().getType().name();
        BlockPosition position = position(event.getBlockPlaced());

        Optional<GeneratorType> generator = runtime.services().generators().types().stream()
                .filter(type -> type.icon().equalsIgnoreCase(material)).findFirst();
        if (generator.isPresent()) {
            GeneratorService.PlacementResult result = runtime.services().generators()
                    .place(island, profile, generator.get().id(), position, 1);
            if (result.success()) {
                runtime.send(player, "generator-placed", "tier", result.state().tier());
            } else if ("limit-reached".equals(result.reason())) {
                event.setCancelled(true);
                runtime.send(player, "generator-limit");
            }
            return;
        }

        Optional<SpawnerType> spawner = runtime.services().spawners().types().stream()
                .filter(type -> type.icon().equalsIgnoreCase(material)).findFirst();
        if (spawner.isPresent()) {
            SpawnerService.PlacementResult result = runtime.services().spawners()
                    .place(island, profile, spawner.get().id(), position, 1);
            if (result.success()) {
                if ("stacked".equals(result.reason())) {
                    runtime.send(player, "spawner-stacked", "amount", result.state().stackSize());
                } else {
                    runtime.send(player, "spawner-placed", "tier", result.state().tier());
                }
            } else {
                event.setCancelled(true);
                if ("limit-reached".equals(result.reason()) || "stack-limit".equals(result.reason())) {
                    runtime.send(player, "spawner-limit");
                } else {
                    runtime.send(player, "gui-locked");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        BlockPosition position = position(event.getBlock());
        runtime.services().generators().remove(position).ifPresent(state ->
                runtime.plugin().runAsync(() -> runtime.profiles().repository().deleteGenerator(state.id())));
        runtime.services().spawners().at(position).ifPresent(state -> {
            runtime.services().spawners().removeOne(position);
            if (runtime.services().spawners().at(position).isEmpty()) {
                runtime.plugin().runAsync(() -> runtime.profiles().repository().deleteSpawner(state.id()));
            }
        });
    }
}
