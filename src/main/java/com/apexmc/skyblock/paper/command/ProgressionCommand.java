package com.apexmc.skyblock.paper.command;

import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import com.apexmc.skyblock.paper.gui.Menus;
import com.apexmc.skyblock.paper.util.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * Every player facing command. One executor serves {@code /progression} and all
 * shortcut commands ({@code /islevel}, {@code /mastery}, {@code /roles},
 * {@code /omnitool}, {@code /generators}, {@code /spawners},
 * {@code /companions}, {@code /skytokens}).
 */
public final class ProgressionCommand implements CommandExecutor, TabCompleter {

    private final SkyblockRuntime runtime;
    private final Menus menus;
    private final AdminCommand admin;

    public ProgressionCommand(SkyblockRuntime runtime, Menus menus, AdminCommand admin) {
        this.runtime = runtime;
        this.menus = menus;
        this.admin = admin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = label.toLowerCase(Locale.ROOT);
        switch (name) {
            case "islevel" -> {
                return islandInfo(sender);
            }
            case "mastery" -> {
                return openMenu(sender, "mastery");
            }
            case "roles" -> {
                return openMenu(sender, "roles");
            }
            case "omnitool" -> {
                return openMenu(sender, "omnitool");
            }
            case "generators" -> {
                return openMenu(sender, "generators");
            }
            case "spawners" -> {
                return openMenu(sender, "spawners");
            }
            case "companions" -> {
                return openMenu(sender, "companions");
            }
            case "skytokens" -> {
                return tokens(sender, args);
            }
            default -> {
                return root(sender, args);
            }
        }
    }

    private boolean root(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return openMenu(sender, "progression");
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "help" -> help(sender);
            case "island", "level" -> islandInfo(sender);
            case "mastery", "roles", "omnitool", "generators", "spawners", "companions" -> openMenu(sender, sub);
            case "tokens" -> tokens(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
            case "admin" -> admin.execute(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
            default -> {
                runtime.send(sender, "unknown-subcommand");
                yield true;
            }
        };
    }

    private boolean help(CommandSender sender) {
        runtime.sendRaw(sender, "&b&lApex SkyBlock Progression");
        runtime.sendRaw(sender, "&7/progression &8- &fopen the overview menu");
        runtime.sendRaw(sender, "&7/islevel &8- &fisland level and unlocks");
        runtime.sendRaw(sender, "&7/mastery &8- &fisland mastery tracks");
        runtime.sendRaw(sender, "&7/roles &8- &fchoose your role");
        runtime.sendRaw(sender, "&7/omnitool &8- &fupgrade your OmniTool");
        runtime.sendRaw(sender, "&7/generators &7| &7/spawners &7| &7/companions");
        runtime.sendRaw(sender, "&7/skytokens [balance|pay <player> <amount>]");
        if (sender.hasPermission("apexskyblock.admin")) {
            runtime.sendRaw(sender, "&7/progression admin &8- &fadministration commands");
        }
        return true;
    }

    private Optional<Player> player(CommandSender sender) {
        if (sender instanceof Player player) {
            return Optional.of(player);
        }
        runtime.send(sender, "player-only");
        return Optional.empty();
    }

    private boolean openMenu(CommandSender sender, String menu) {
        Optional<Player> maybePlayer = player(sender);
        if (maybePlayer.isEmpty()) {
            return true;
        }
        Player player = maybePlayer.get();
        if (!player.hasPermission("apexskyblock.use")) {
            runtime.send(player, "no-permission");
            return true;
        }
        switch (menu) {
            case "mastery" -> menus.mastery().open(player);
            case "roles" -> menus.roles().open(player);
            case "generators" -> menus.generators().open(player);
            case "spawners" -> menus.spawners().open(player);
            case "companions" -> menus.companions().open(player);
            case "omnitool" -> runtime.profile(player).ifPresent(profile -> {
                String roleId = runtime.services().roles().activeRoleId(profile);
                String toolId = runtime.services().omniTools().toolForRole(roleId)
                        .map(tool -> tool.id()).orElse(roleId);
                menus.omniTool(toolId).open(player);
            });
            default -> menus.progression().open(player);
        }
        return true;
    }

    private boolean islandInfo(CommandSender sender) {
        Optional<Player> maybePlayer = player(sender);
        if (maybePlayer.isEmpty()) {
            return true;
        }
        Player player = maybePlayer.get();
        Optional<PlayerProfile> profile = runtime.profile(player);
        Optional<IslandProfile> island = profile.flatMap(runtime::island);
        if (island.isEmpty()) {
            runtime.send(player, "unknown-player");
            return true;
        }
        var track = runtime.services().islands().track(island.get());
        runtime.sendRaw(player, runtime.messages().prefix() + "&bIsland level &e" + track.level()
                + "&7/&e" + runtime.services().islands().config().maxLevel());
        runtime.sendRaw(player, "&7" + Text.bar(track.progress(), 25, "&a", "&8")
                + " &e" + Text.percent(track.progress()));
        runtime.sendRaw(player, "&7XP: &e" + track.totalXp() + " &7(next level in &e"
                + track.xpToNextLevel() + "&7)");
        runtime.services().islands().config().level(track.level() + 1).ifPresent(next -> {
            runtime.sendRaw(player, "&7Next unlocks:");
            next.unlocks().forEach(unlock -> runtime.sendRaw(player, "&d \u2726 " + SkyblockRuntime.prettify(unlock)));
        });
        return true;
    }

    private boolean tokens(CommandSender sender, String[] args) {
        Optional<Player> maybePlayer = player(sender);
        if (maybePlayer.isEmpty()) {
            return true;
        }
        Player player = maybePlayer.get();
        Optional<PlayerProfile> maybeProfile = runtime.profile(player);
        if (maybeProfile.isEmpty()) {
            runtime.send(player, "unknown-player");
            return true;
        }
        PlayerProfile profile = maybeProfile.get();
        if (args.length >= 3 && args[0].equalsIgnoreCase("pay")) {
            Player target = org.bukkit.Bukkit.getPlayer(args[1]);
            if (target == null) {
                runtime.send(player, "unknown-player");
                return true;
            }
            long amount = parseAmount(args[2]);
            if (amount <= 0) {
                runtime.send(player, "invalid-amount");
                return true;
            }
            Optional<PlayerProfile> targetProfile = runtime.profile(target);
            if (targetProfile.isEmpty()) {
                runtime.send(player, "unknown-player");
                return true;
            }
            var transaction = runtime.services().tokens().transfer(profile, targetProfile.get(), amount);
            if (!transaction.success()) {
                runtime.send(player, "tokens-insufficient", "amount", amount,
                        "currency", runtime.services().tokens().displayNamePlural());
                return true;
            }
            runtime.send(player, "tokens-spent", "amount", amount,
                    "currency", runtime.services().tokens().displayNamePlural());
            runtime.send(target, "tokens-received", "amount", amount,
                    "currency", runtime.services().tokens().displayNamePlural());
            runtime.fireTokenChange(profile.playerId(), -amount, profile.tokens(), "pay");
            runtime.fireTokenChange(targetProfile.get().playerId(), amount, targetProfile.get().tokens(), "pay");
            return true;
        }
        runtime.send(player, "tokens-balance", "amount", profile.tokens(),
                "currency", runtime.services().tokens().displayNamePlural());
        return true;
    }

    private long parseAmount(String raw) {
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return -1L;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> options = new ArrayList<>();
        String name = label.toLowerCase(Locale.ROOT);
        if (name.equals("skytokens")) {
            if (args.length == 1) {
                options.add("balance");
                options.add("pay");
            }
            return filter(options, args);
        }
        if (name.equals("progression")) {
            if (args.length == 1) {
                options.addAll(List.of("help", "island", "mastery", "roles", "omnitool",
                        "generators", "spawners", "companions", "tokens"));
                if (sender.hasPermission("apexskyblock.admin")) {
                    options.add("admin");
                }
                return filter(options, args);
            }
            if (args[0].equalsIgnoreCase("admin")) {
                return admin.tabComplete(sender, java.util.Arrays.copyOfRange(args, 1, args.length));
            }
        }
        return options;
    }

    private List<String> filter(List<String> options, String[] args) {
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
