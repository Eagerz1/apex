package com.apexmc.skyblock.core.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Public API other plugins can depend on. Obtain it from the Bukkit services
 * manager:
 *
 * <pre>{@code
 * RegisteredServiceProvider<SkyblockProgressionAPI> provider =
 *         Bukkit.getServicesManager().getRegistration(SkyblockProgressionAPI.class);
 * }</pre>
 *
 * <p>All methods are safe to call from the main thread and only read cached
 * data; unknown players simply return empty/zero values.</p>
 */
public interface SkyblockProgressionAPI {

    /** Island progression level of a player's island. */
    int islandLevel(UUID playerId);

    /** Total island XP of a player's island. */
    long islandXp(UUID playerId);

    /** Mastery level in a category (returns 0 for unknown categories). */
    int masteryLevel(UUID playerId, String categoryId);

    /** Level of a specific role. */
    int roleLevel(UUID playerId, String roleId);

    /** Currently selected role identifier. */
    Optional<String> activeRole(UUID playerId);

    /** Sky Token balance. */
    long tokenBalance(UUID playerId);

    /** Grants Sky Tokens; returns the new balance. */
    long giveTokens(UUID playerId, long amount);

    /** Takes Sky Tokens; returns {@code false} when the player cannot afford it. */
    boolean takeTokens(UUID playerId, long amount);

    /** Whether an island unlock is available to the player. */
    boolean hasUnlock(UUID playerId, String unlockId);

    /** Level of an OmniTool upgrade node. */
    int omniToolNodeLevel(UUID playerId, String toolId, String nodeId);

    /** Current season identifier. */
    String seasonId();
}
