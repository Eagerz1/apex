package com.apexmc.skyblock.paper.hook;

import com.apexmc.skyblock.core.api.SkyblockProgressionAPI;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import java.util.Locale;
import org.bukkit.OfflinePlayer;

/**
 * PlaceholderAPI expansion. The class is only loaded when PlaceholderAPI is
 * installed, so the plugin runs perfectly without it.
 *
 * <p>Supported placeholders:
 * {@code %apexsb_island_level%}, {@code %apexsb_island_xp%},
 * {@code %apexsb_role%}, {@code %apexsb_role_level%},
 * {@code %apexsb_mastery_<category>%}, {@code %apexsb_tokens%},
 * {@code %apexsb_season%}, {@code %apexsb_unlocked_<unlock>%}.</p>
 */
public final class PlaceholderHook extends me.clip.placeholderapi.expansion.PlaceholderExpansion {

    private final SkyblockRuntime runtime;
    private final SkyblockProgressionAPI api;

    public PlaceholderHook(SkyblockRuntime runtime, SkyblockProgressionAPI api) {
        this.runtime = runtime;
        this.api = api;
    }

    @Override
    public String getIdentifier() {
        return "apexsb";
    }

    @Override
    public String getAuthor() {
        return "Apex";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }
        String key = params.toLowerCase(Locale.ROOT);
        java.util.UUID id = player.getUniqueId();
        if (key.equals("island_level")) {
            return String.valueOf(api.islandLevel(id));
        }
        if (key.equals("island_xp")) {
            return String.valueOf(api.islandXp(id));
        }
        if (key.equals("role")) {
            return api.activeRole(id).map(SkyblockRuntime::prettify).orElse("");
        }
        if (key.equals("role_level")) {
            return String.valueOf(api.activeRole(id).map(role -> api.roleLevel(id, role)).orElse(0));
        }
        if (key.equals("tokens") || key.equals(runtime.services().config().currency().placeholderName())) {
            return String.valueOf(api.tokenBalance(id));
        }
        if (key.equals("season")) {
            return api.seasonId();
        }
        if (key.startsWith("mastery_")) {
            return String.valueOf(api.masteryLevel(id, key.substring("mastery_".length())));
        }
        if (key.startsWith("role_level_")) {
            return String.valueOf(api.roleLevel(id, key.substring("role_level_".length())));
        }
        if (key.startsWith("unlocked_")) {
            return String.valueOf(api.hasUnlock(id, key.substring("unlocked_".length())));
        }
        return null;
    }
}
