package com.apexmc.skyblock.paper.listener;

import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Loads and unloads progression data. Every database access happens on the
 * plugin's async executor; the main thread only ever sees cached objects.
 */
public final class ConnectionListener implements Listener {

    private final SkyblockRuntime runtime;

    public ConnectionListener(SkyblockRuntime runtime) {
        this.runtime = runtime;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        runtime.profiles().loadPlayer(playerId).thenCompose(profile -> {
            UUID islandOwner = runtime.islandResolver().islandOwnerOf(playerId).orElse(playerId);
            return runtime.profiles().loadIslandForOwner(islandOwner)
                    .thenApply(island -> new Object[]{profile, island});
        }).thenAccept(loaded -> {
            PlayerProfile profile = (PlayerProfile) loaded[0];
            IslandProfile island = (IslandProfile) loaded[1];
            profile.islandId(island.islandId());
            applySeason(profile, island);
            runtime.services().omniTools().sanitize(profile);
            runtime.services().companions().sanitize(profile);
        }).exceptionally(error -> {
            runtime.plugin().getLogger().log(Level.SEVERE,
                    "Failed to load progression data for " + playerId, error);
            return null;
        });
    }

    private void applySeason(PlayerProfile profile, IslandProfile island) {
        var seasons = runtime.services().seasons();
        if (seasons.needsReset(profile) || seasons.needsReset(island)) {
            seasons.applyTo(profile);
            seasons.applyTo(island);
            if (seasons.resetsGenerators()) {
                runtime.services().generators().removeIsland(island.islandId());
            }
            if (seasons.resetsSpawners()) {
                runtime.services().spawners().removeIsland(island.islandId());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        runtime.profiles().cachedPlayer(playerId)
                .flatMap(runtime::island)
                .ifPresent(island -> runtime.profiles().saveIsland(island));
        runtime.profiles().unloadPlayer(playerId);
    }
}
