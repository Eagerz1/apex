package com.apexmc.skyblock.paper.api;

import com.apexmc.skyblock.core.api.SkyblockProgressionAPI;
import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import java.util.Optional;
import java.util.UUID;

/** Default {@link SkyblockProgressionAPI} implementation backed by the cache. */
public final class ApiImpl implements SkyblockProgressionAPI {

    private final SkyblockRuntime runtime;

    public ApiImpl(SkyblockRuntime runtime) {
        this.runtime = runtime;
    }

    private Optional<PlayerProfile> profile(UUID playerId) {
        return runtime.profiles().cachedPlayer(playerId);
    }

    private Optional<IslandProfile> island(UUID playerId) {
        return profile(playerId).flatMap(runtime::island);
    }

    @Override
    public int islandLevel(UUID playerId) {
        return island(playerId).map(island -> runtime.services().islands().levelOf(island)).orElse(0);
    }

    @Override
    public long islandXp(UUID playerId) {
        return island(playerId).map(IslandProfile::islandXp).orElse(0L);
    }

    @Override
    public int masteryLevel(UUID playerId, String categoryId) {
        return island(playerId).map(island -> runtime.services().mastery().levelOf(island, categoryId)).orElse(0);
    }

    @Override
    public int roleLevel(UUID playerId, String roleId) {
        return profile(playerId).map(profile -> runtime.services().roles().levelOf(profile, roleId)).orElse(0);
    }

    @Override
    public Optional<String> activeRole(UUID playerId) {
        return profile(playerId).map(profile -> runtime.services().roles().activeRoleId(profile));
    }

    @Override
    public long tokenBalance(UUID playerId) {
        return profile(playerId).map(PlayerProfile::tokens).orElse(0L);
    }

    @Override
    public long giveTokens(UUID playerId, long amount) {
        return profile(playerId).map(profile -> {
            var transaction = runtime.services().tokens().give(profile, amount);
            runtime.fireTokenChange(playerId, transaction.delta(), transaction.newBalance(), "api");
            return transaction.newBalance();
        }).orElse(0L);
    }

    @Override
    public boolean takeTokens(UUID playerId, long amount) {
        return profile(playerId).map(profile -> {
            var transaction = runtime.services().tokens().take(profile, amount);
            runtime.fireTokenChange(playerId, transaction.delta(), transaction.newBalance(), "api");
            return transaction.success();
        }).orElse(false);
    }

    @Override
    public boolean hasUnlock(UUID playerId, String unlockId) {
        return island(playerId).map(island -> runtime.services().islands().isUnlocked(island, unlockId)).orElse(false);
    }

    @Override
    public int omniToolNodeLevel(UUID playerId, String toolId, String nodeId) {
        return profile(playerId)
                .map(profile -> runtime.services().omniTools().nodeLevel(profile, toolId, nodeId))
                .orElse(0);
    }

    @Override
    public String seasonId() {
        return runtime.services().seasons().currentSeasonId();
    }
}
