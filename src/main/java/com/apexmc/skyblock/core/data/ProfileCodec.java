package com.apexmc.skyblock.core.data;

import com.apexmc.skyblock.core.util.Codec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Converts profiles to and from a flat {@code String} map. Every storage
 * backend (SQLite, MySQL, tests) shares this codec, so serialization is
 * implemented and tested exactly once.
 */
public final class ProfileCodec {

    private ProfileCodec() {
    }

    public static Map<String, String> encodePlayer(PlayerProfile profile) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("player_id", profile.playerId().toString());
        row.put("island_id", profile.islandId().map(UUID::toString).orElse(""));
        row.put("season_id", profile.seasonId());
        row.put("active_role", profile.activeRoleId().orElse(""));
        row.put("tokens", Long.toString(profile.tokens()));
        row.put("role_xp", Codec.encode(profile.roleXpView()));
        row.put("stats", Codec.encode(profile.statsView()));
        row.put("tool_upgrades", encodeToolUpgrades(profile));
        row.put("companions", encodeCompanions(profile));
        row.put("active_companion", profile.activeCompanionId().map(UUID::toString).orElse(""));
        return row;
    }

    public static PlayerProfile decodePlayer(Map<String, String> row) {
        UUID playerId = UUID.fromString(require(row, "player_id"));
        PlayerProfile profile = new PlayerProfile(playerId, row.getOrDefault("season_id", "season-1"));
        String islandId = row.getOrDefault("island_id", "");
        if (!islandId.isBlank()) {
            profile.islandId(UUID.fromString(islandId));
        }
        String activeRole = row.getOrDefault("active_role", "");
        if (!activeRole.isBlank()) {
            profile.activeRoleId(activeRole);
        }
        profile.tokens(parseLong(row.get("tokens")));
        for (Map.Entry<String, String> entry : Codec.decode(row.getOrDefault("role_xp", "")).entrySet()) {
            profile.roleXp(entry.getKey(), parseLong(entry.getValue()));
        }
        for (Map.Entry<String, String> entry : Codec.decode(row.getOrDefault("stats", "")).entrySet()) {
            profile.stat(entry.getKey(), parseLong(entry.getValue()));
        }
        decodeToolUpgrades(profile, row.getOrDefault("tool_upgrades", ""));
        decodeCompanions(profile, row.getOrDefault("companions", ""));
        String activeCompanion = row.getOrDefault("active_companion", "");
        if (!activeCompanion.isBlank()) {
            try {
                UUID id = UUID.fromString(activeCompanion);
                if (profile.companion(id).isPresent()) {
                    profile.activeCompanionId(id);
                }
            } catch (IllegalArgumentException ignored) {
                // Corrupt value: leave no active companion rather than failing the load.
            }
        }
        profile.clearDirty();
        return profile;
    }

    public static Map<String, String> encodeIsland(IslandProfile island) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("island_id", island.islandId().toString());
        row.put("owner_id", island.ownerId() == null ? "" : island.ownerId().toString());
        row.put("season_id", island.seasonId());
        row.put("island_xp", Long.toString(island.islandXp()));
        row.put("mastery_xp", Codec.encode(island.masteryXpView()));
        row.put("unlocks", String.join(",", island.grantedUnlocks()));
        return row;
    }

    public static IslandProfile decodeIsland(Map<String, String> row) {
        UUID islandId = UUID.fromString(require(row, "island_id"));
        String owner = row.getOrDefault("owner_id", "");
        IslandProfile island = new IslandProfile(islandId,
                owner.isBlank() ? null : UUID.fromString(owner),
                row.getOrDefault("season_id", "season-1"));
        island.islandXp(parseLong(row.get("island_xp")));
        for (Map.Entry<String, String> entry : Codec.decode(row.getOrDefault("mastery_xp", "")).entrySet()) {
            island.masteryXp(entry.getKey(), parseLong(entry.getValue()));
        }
        String unlocks = row.getOrDefault("unlocks", "");
        if (!unlocks.isBlank()) {
            for (String unlock : unlocks.split(",")) {
                if (!unlock.isBlank()) {
                    island.grantedUnlocks().add(unlock.trim());
                }
            }
        }
        island.clearDirty();
        return island;
    }

    private static String encodeToolUpgrades(PlayerProfile profile) {
        Map<String, String> flattened = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Integer>> tool : profile.toolUpgradesView().entrySet()) {
            for (Map.Entry<String, Integer> node : tool.getValue().entrySet()) {
                flattened.put(tool.getKey() + "/" + node.getKey(), String.valueOf(node.getValue()));
            }
        }
        return Codec.encode(flattened);
    }

    private static void decodeToolUpgrades(PlayerProfile profile, String raw) {
        for (Map.Entry<String, String> entry : Codec.decode(raw).entrySet()) {
            int split = entry.getKey().indexOf('/');
            if (split <= 0 || split == entry.getKey().length() - 1) {
                continue;
            }
            String toolId = entry.getKey().substring(0, split);
            String nodeId = entry.getKey().substring(split + 1);
            int level = (int) parseLong(entry.getValue());
            if (level > 0) {
                profile.toolUpgrades(toolId).put(nodeId, level);
            }
        }
    }

    private static String encodeCompanions(PlayerProfile profile) {
        List<String> encoded = new ArrayList<>();
        for (CompanionInstance companion : profile.companions()) {
            encoded.add(String.join("|",
                    companion.instanceId().toString(),
                    companion.definitionId(),
                    companion.rarityId(),
                    Long.toString(companion.xp())));
        }
        return String.join(",", encoded);
    }

    private static void decodeCompanions(PlayerProfile profile, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        for (String entry : raw.split(",")) {
            String[] parts = entry.split("\\|");
            if (parts.length != 4) {
                continue;
            }
            try {
                profile.companions().add(new CompanionInstance(
                        UUID.fromString(parts[0]), parts[1], parts[2], parseLong(parts[3])));
            } catch (IllegalArgumentException ignored) {
                // Skip the corrupt companion instead of discarding the whole profile.
            }
        }
    }

    private static String require(Map<String, String> row, String key) {
        String value = row.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing column '" + key + "' in stored row");
        }
        return value;
    }

    private static long parseLong(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
