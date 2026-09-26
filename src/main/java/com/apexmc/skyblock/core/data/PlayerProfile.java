package com.apexmc.skyblock.core.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * All persisted per-player progression. Everything is keyed by identifier, never
 * by display name, so renaming a role or a tool node in configuration never
 * corrupts stored data.
 */
public final class PlayerProfile {

    private final UUID playerId;
    private UUID islandId;
    private String seasonId;
    private String activeRoleId;
    private long tokens;
    private final Map<String, Long> roleXp = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> toolUpgrades = new LinkedHashMap<>();
    private final Map<String, Long> stats = new LinkedHashMap<>();
    private final List<CompanionInstance> companions = new ArrayList<>();
    private UUID activeCompanionId;
    private volatile boolean dirty;

    public PlayerProfile(UUID playerId, String seasonId) {
        this.playerId = playerId;
        this.seasonId = seasonId;
    }

    public UUID playerId() {
        return playerId;
    }

    public Optional<UUID> islandId() {
        return Optional.ofNullable(islandId);
    }

    public void islandId(UUID islandId) {
        this.islandId = islandId;
        markDirty();
    }

    public String seasonId() {
        return seasonId;
    }

    public void seasonId(String seasonId) {
        this.seasonId = seasonId;
        markDirty();
    }

    public Optional<String> activeRoleId() {
        return Optional.ofNullable(activeRoleId);
    }

    public void activeRoleId(String roleId) {
        this.activeRoleId = roleId;
        markDirty();
    }

    public long tokens() {
        return tokens;
    }

    public void tokens(long tokens) {
        this.tokens = Math.max(0L, tokens);
        markDirty();
    }

    public long roleXp(String roleId) {
        return roleXp.getOrDefault(roleId, 0L);
    }

    public void roleXp(String roleId, long xp) {
        roleXp.put(roleId, Math.max(0L, xp));
        markDirty();
    }

    public Map<String, Long> roleXpView() {
        return java.util.Collections.unmodifiableMap(roleXp);
    }

    public Map<String, Integer> toolUpgrades(String roleId) {
        return toolUpgrades.computeIfAbsent(roleId, key -> new LinkedHashMap<>());
    }

    public Map<String, Map<String, Integer>> toolUpgradesView() {
        return java.util.Collections.unmodifiableMap(toolUpgrades);
    }

    public long stat(String key) {
        return stats.getOrDefault(key, 0L);
    }

    public void stat(String key, long value) {
        stats.put(key, Math.max(0L, value));
        markDirty();
    }

    public void incrementStat(String key, long delta) {
        stat(key, com.apexmc.skyblock.core.util.Numbers.safeAdd(stat(key), delta));
    }

    public Map<String, Long> statsView() {
        return java.util.Collections.unmodifiableMap(stats);
    }

    public List<CompanionInstance> companions() {
        return companions;
    }

    public Optional<CompanionInstance> companion(UUID instanceId) {
        return companions.stream().filter(companion -> companion.instanceId().equals(instanceId)).findFirst();
    }

    public Optional<UUID> activeCompanionId() {
        return Optional.ofNullable(activeCompanionId);
    }

    public void activeCompanionId(UUID activeCompanionId) {
        this.activeCompanionId = activeCompanionId;
        markDirty();
    }

    public boolean dirty() {
        return dirty;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void clearDirty() {
        this.dirty = false;
    }
}
