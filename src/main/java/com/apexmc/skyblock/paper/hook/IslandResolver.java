package com.apexmc.skyblock.paper.hook;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves which island a player belongs to.
 *
 * <p>The default implementation treats every player as the owner of their own
 * island, which works on any server. Servers running a dedicated SkyBlock
 * plugin can register a different resolver through the plugin instance so
 * island progression is shared by island members.</p>
 */
public interface IslandResolver {

    /** Island identifier for a player, or empty when the player has no island. */
    Optional<UUID> islandOwnerOf(UUID playerId);

    /** Resolver used when no SkyBlock plugin hook is installed. */
    static IslandResolver selfOwned() {
        return Optional::of;
    }
}
