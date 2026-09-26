package com.apexmc.skyblock.paper;

import com.apexmc.skyblock.core.config.Messages;
import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.data.ProfileManager;
import com.apexmc.skyblock.core.progression.IslandLevel;
import com.apexmc.skyblock.core.progression.LevelUpResult;
import com.apexmc.skyblock.core.service.ProgressionServices;
import com.apexmc.skyblock.paper.api.IslandLevelUpEvent;
import com.apexmc.skyblock.paper.api.MasteryLevelUpEvent;
import com.apexmc.skyblock.paper.api.RoleLevelUpEvent;
import com.apexmc.skyblock.paper.api.TokenBalanceChangeEvent;
import com.apexmc.skyblock.paper.hook.IslandResolver;
import com.apexmc.skyblock.paper.util.Text;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Glue between the Bukkit world and the dependency free core: profile lookups,
 * reward payouts, messaging and API events. Listeners, menus and commands all
 * talk to this class instead of reaching into services directly.
 */
public final class SkyblockRuntime {

    private final ApexSkyblockPlugin plugin;
    private ProgressionServices services;
    private ProfileManager profiles;
    private IslandResolver islandResolver = IslandResolver.selfOwned();

    public SkyblockRuntime(ApexSkyblockPlugin plugin, ProgressionServices services, ProfileManager profiles) {
        this.plugin = plugin;
        this.services = services;
        this.profiles = profiles;
    }

    public ApexSkyblockPlugin plugin() {
        return plugin;
    }

    public ProgressionServices services() {
        return services;
    }

    public void services(ProgressionServices services) {
        this.services = services;
    }

    public ProfileManager profiles() {
        return profiles;
    }

    public void profiles(ProfileManager profiles) {
        this.profiles = profiles;
    }

    public IslandResolver islandResolver() {
        return islandResolver;
    }

    public void islandResolver(IslandResolver islandResolver) {
        this.islandResolver = islandResolver == null ? IslandResolver.selfOwned() : islandResolver;
    }

    public Messages messages() {
        return services.config().messages();
    }

    public Optional<PlayerProfile> profile(Player player) {
        return player == null ? Optional.empty() : profiles.cachedPlayer(player.getUniqueId());
    }

    public Optional<IslandProfile> island(PlayerProfile profile) {
        return profile.islandId().flatMap(profiles::cachedIsland);
    }

    public Optional<IslandProfile> island(Player player) {
        return profile(player).flatMap(this::island);
    }

    public void send(CommandSender sender, String key, Object... placeholders) {
        if (sender != null) {
            sender.sendMessage(Text.colour(messages().prefixed(key, placeholders)));
        }
    }

    public void sendRaw(CommandSender sender, String message) {
        if (sender != null) {
            sender.sendMessage(Text.colour(message));
        }
    }

    /**
     * Applies island level-up side effects: rewards, unlock messages and the
     * public {@link IslandLevelUpEvent}. Safe to call for every XP gain.
     */
    public void handleIslandResult(Player player, IslandProfile island, LevelUpResult result) {
        if (!result.leveledUp()) {
            return;
        }
        for (IslandLevel level : services.islands().levelsBetween(result.previousLevel(), result.newLevel())) {
            if (player != null) {
                send(player, "island-level-up", "level", level.level(), "title", level.title());
                level.unlocks().forEach(unlock -> send(player, "island-unlock", "unlock", prettify(unlock)));
            }
            if (level.tokenReward() > 0) {
                PlayerProfile owner = ownerProfile(island);
                if (owner != null) {
                    long before = owner.tokens();
                    services.tokens().give(owner, level.tokenReward());
                    fireTokenChange(owner.playerId(), owner.tokens() - before, owner.tokens(), "island-level");
                }
            }
            if (level.moneyReward() > 0) {
                services.economy().deposit(island.ownerId(), level.moneyReward());
            }
            for (String command : level.commands()) {
                String parsed = command.replace("%player%", player == null ? "" : player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
        }
        callEvent(new IslandLevelUpEvent(island.islandId(), island.ownerId(), result.previousLevel(), result.newLevel()));
    }

    public void handleRoleResult(Player player, String roleId, LevelUpResult result) {
        if (!result.leveledUp()) {
            return;
        }
        if (player != null) {
            send(player, "role-level-up", "role", prettify(roleId), "level", result.newLevel());
            callEvent(new RoleLevelUpEvent(player.getUniqueId(), roleId, result.previousLevel(), result.newLevel()));
        }
    }

    public void handleMasteryResult(Player player, IslandProfile island, String categoryId, LevelUpResult result) {
        if (!result.leveledUp()) {
            return;
        }
        if (player != null) {
            send(player, "mastery-level-up", "category", prettify(categoryId), "level", result.newLevel());
        }
        callEvent(new MasteryLevelUpEvent(island.islandId(), categoryId, result.previousLevel(), result.newLevel()));
    }

    public void fireTokenChange(UUID playerId, long delta, long balance, String reason) {
        if (delta != 0) {
            callEvent(new TokenBalanceChangeEvent(playerId, delta, balance, reason));
        }
    }

    private void callEvent(org.bukkit.event.Event event) {
        if (Bukkit.getPluginManager() != null) {
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    private PlayerProfile ownerProfile(IslandProfile island) {
        return island.ownerId() == null ? null : profiles.cachedPlayer(island.ownerId()).orElse(null);
    }

    /** Turns {@code generator_tier_2} into {@code Generator Tier 2} for messages. */
    public static String prettify(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        String[] parts = id.replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
