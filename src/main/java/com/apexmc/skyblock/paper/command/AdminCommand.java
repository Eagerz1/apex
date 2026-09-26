package com.apexmc.skyblock.paper.command;

import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Administration commands: XP and level manipulation, token management,
 * configuration reload, inspection and debug output.
 */
public final class AdminCommand {

    private static final String PERMISSION = "apexskyblock.admin";

    private final SkyblockRuntime runtime;

    public AdminCommand(SkyblockRuntime runtime) {
        this.runtime = runtime;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            runtime.send(sender, "no-permission");
            return true;
        }
        if (args.length == 0) {
            usage(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "reload" -> reload(sender);
            case "debug" -> debug(sender);
            case "inspect" -> inspect(sender, args);
            case "islandxp" -> islandXp(sender, args);
            case "islandlevel" -> islandLevel(sender, args);
            case "rolexp" -> roleXp(sender, args);
            case "rolelevel" -> roleLevel(sender, args);
            case "masteryxp" -> masteryXp(sender, args);
            case "tokens" -> tokens(sender, args);
            case "save" -> save(sender);
            default -> {
                usage(sender);
                yield true;
            }
        };
    }

    private void usage(CommandSender sender) {
        runtime.sendRaw(sender, "&b&lAdmin commands");
        runtime.sendRaw(sender, "&7/progression admin reload");
        runtime.sendRaw(sender, "&7/progression admin save");
        runtime.sendRaw(sender, "&7/progression admin debug");
        runtime.sendRaw(sender, "&7/progression admin inspect <player>");
        runtime.sendRaw(sender, "&7/progression admin islandxp <player> <add|set|remove> <amount>");
        runtime.sendRaw(sender, "&7/progression admin islandlevel <player> <level>");
        runtime.sendRaw(sender, "&7/progression admin rolexp <player> <role> <add|set|remove> <amount>");
        runtime.sendRaw(sender, "&7/progression admin rolelevel <player> <role> <level>");
        runtime.sendRaw(sender, "&7/progression admin masteryxp <player> <category> <add|set> <amount>");
        runtime.sendRaw(sender, "&7/progression admin tokens <player> <give|take|set> <amount>");
    }

    private boolean reload(CommandSender sender) {
        long start = System.currentTimeMillis();
        try {
            runtime.plugin().reloadProgression();
            runtime.send(sender, "reloaded", "ms", System.currentTimeMillis() - start);
        } catch (RuntimeException ex) {
            runtime.send(sender, "reload-failed", "error", ex.getMessage());
        }
        return true;
    }

    private boolean save(CommandSender sender) {
        int saved = runtime.profiles().flushBlocking();
        runtime.sendRaw(sender, runtime.messages().prefix() + "&aSaved &e" + saved + " &aprofiles.");
        return true;
    }

    private boolean debug(CommandSender sender) {
        var services = runtime.services();
        runtime.sendRaw(sender, "&b&lApex SkyBlock debug");
        runtime.sendRaw(sender, "&7Season: &e" + services.seasons().currentSeasonId()
                + " &7(xp x" + services.seasons().xpMultiplier() + ")");
        runtime.sendRaw(sender, "&7Island levels: &e" + services.islands().config().maxLevel());
        runtime.sendRaw(sender, "&7Roles: &e" + services.roles().registry().ids());
        runtime.sendRaw(sender, "&7Mastery categories: &e" + services.mastery().categories().size());
        runtime.sendRaw(sender, "&7OmniTools: &e" + services.omniTools().all().size());
        runtime.sendRaw(sender, "&7Generators loaded: &e" + services.generators().all().size());
        runtime.sendRaw(sender, "&7Spawners loaded: &e" + services.spawners().all().size());
        runtime.sendRaw(sender, "&7Cached players: &e" + runtime.profiles().loadedPlayers().size()
                + " &7islands: &e" + runtime.profiles().loadedIslands().size());
        runtime.sendRaw(sender, "&7Economy: &e" + (services.economy().available() ? "Vault" : "none")
                + " &7(missing policy: " + services.economy().policy() + ")");
        return true;
    }

    private Optional<PlayerProfile> target(CommandSender sender, String name) {
        Player player = Bukkit.getPlayer(name);
        if (player == null) {
            runtime.send(sender, "unknown-player");
            return Optional.empty();
        }
        Optional<PlayerProfile> profile = runtime.profile(player);
        if (profile.isEmpty()) {
            runtime.send(sender, "unknown-player");
        }
        return profile;
    }

    private boolean inspect(CommandSender sender, String[] args) {
        if (args.length < 2) {
            usage(sender);
            return true;
        }
        Optional<PlayerProfile> maybeProfile = target(sender, args[1]);
        if (maybeProfile.isEmpty()) {
            return true;
        }
        PlayerProfile profile = maybeProfile.get();
        runtime.sendRaw(sender, "&b&lProfile &f" + args[1]);
        runtime.sendRaw(sender, "&7Season: &e" + profile.seasonId());
        runtime.sendRaw(sender, "&7Active role: &e" + runtime.services().roles().activeRoleId(profile));
        runtime.sendRaw(sender, "&7Tokens: &e" + profile.tokens());
        profile.roleXpView().forEach((role, xp) ->
                runtime.sendRaw(sender, "&7 role " + role + ": &e" + xp + " xp &7(level "
                        + runtime.services().roles().levelOf(profile, role) + ")"));
        profile.toolUpgradesView().forEach((tool, nodes) ->
                runtime.sendRaw(sender, "&7 tool " + tool + ": &e" + nodes));
        runtime.sendRaw(sender, "&7Companions: &e" + profile.companions().size());
        runtime.island(profile).ifPresent(island -> {
            runtime.sendRaw(sender, "&7Island level: &e" + runtime.services().islands().levelOf(island)
                    + " &7(" + island.islandXp() + " xp)");
            runtime.sendRaw(sender, "&7Mastery: &e" + island.masteryXpView());
            runtime.sendRaw(sender, "&7Extra unlocks: &e" + island.grantedUnlocks());
        });
        return true;
    }

    private long amount(CommandSender sender, String raw) {
        try {
            long value = Long.parseLong(raw.trim());
            if (value < 0) {
                runtime.send(sender, "invalid-amount");
                return -1;
            }
            return value;
        } catch (NumberFormatException ex) {
            runtime.send(sender, "invalid-amount");
            return -1;
        }
    }

    private boolean islandXp(CommandSender sender, String[] args) {
        if (args.length < 4) {
            usage(sender);
            return true;
        }
        Optional<PlayerProfile> profile = target(sender, args[1]);
        if (profile.isEmpty()) {
            return true;
        }
        Optional<IslandProfile> island = runtime.island(profile.get());
        if (island.isEmpty()) {
            runtime.send(sender, "unknown-player");
            return true;
        }
        long value = amount(sender, args[3]);
        if (value < 0) {
            return true;
        }
        var islands = runtime.services().islands();
        var result = switch (args[2].toLowerCase(Locale.ROOT)) {
            case "set" -> islands.setXp(island.get(), value);
            case "remove" -> islands.removeXp(island.get(), value);
            default -> islands.addXp(island.get(), value);
        };
        runtime.handleIslandResult(Bukkit.getPlayer(args[1]), island.get(), result);
        runtime.sendRaw(sender, runtime.messages().prefix() + "&aIsland XP is now &e" + island.get().islandXp()
                + " &7(level " + islands.levelOf(island.get()) + ")");
        return true;
    }

    private boolean islandLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            usage(sender);
            return true;
        }
        Optional<PlayerProfile> profile = target(sender, args[1]);
        if (profile.isEmpty()) {
            return true;
        }
        Optional<IslandProfile> island = runtime.island(profile.get());
        if (island.isEmpty()) {
            runtime.send(sender, "unknown-player");
            return true;
        }
        long level = amount(sender, args[2]);
        if (level < 0) {
            return true;
        }
        var result = runtime.services().islands().setLevel(island.get(), (int) level);
        runtime.handleIslandResult(Bukkit.getPlayer(args[1]), island.get(), result);
        runtime.sendRaw(sender, runtime.messages().prefix() + "&aIsland level set to &e"
                + runtime.services().islands().levelOf(island.get()));
        return true;
    }

    private boolean roleXp(CommandSender sender, String[] args) {
        if (args.length < 5) {
            usage(sender);
            return true;
        }
        Optional<PlayerProfile> profile = target(sender, args[1]);
        if (profile.isEmpty()) {
            return true;
        }
        if (!runtime.services().roles().registry().contains(args[2])) {
            runtime.send(sender, "role-unknown");
            return true;
        }
        long value = amount(sender, args[4]);
        if (value < 0) {
            return true;
        }
        var roles = runtime.services().roles();
        var result = switch (args[3].toLowerCase(Locale.ROOT)) {
            case "set" -> roles.setXp(profile.get(), args[2], value);
            case "remove" -> roles.removeXp(profile.get(), args[2], value);
            default -> roles.addXp(profile.get(), args[2], value);
        };
        runtime.handleRoleResult(Bukkit.getPlayer(args[1]), args[2], result);
        runtime.sendRaw(sender, runtime.messages().prefix() + "&aRole &e" + args[2] + " &ais now level &e"
                + roles.levelOf(profile.get(), args[2]));
        return true;
    }

    private boolean roleLevel(CommandSender sender, String[] args) {
        if (args.length < 4) {
            usage(sender);
            return true;
        }
        Optional<PlayerProfile> profile = target(sender, args[1]);
        if (profile.isEmpty()) {
            return true;
        }
        if (!runtime.services().roles().registry().contains(args[2])) {
            runtime.send(sender, "role-unknown");
            return true;
        }
        long level = amount(sender, args[3]);
        if (level < 0) {
            return true;
        }
        var result = runtime.services().roles().setLevel(profile.get(), args[2], (int) level);
        runtime.handleRoleResult(Bukkit.getPlayer(args[1]), args[2], result);
        runtime.sendRaw(sender, runtime.messages().prefix() + "&aRole level set to &e"
                + runtime.services().roles().levelOf(profile.get(), args[2]));
        return true;
    }

    private boolean masteryXp(CommandSender sender, String[] args) {
        if (args.length < 5) {
            usage(sender);
            return true;
        }
        Optional<PlayerProfile> profile = target(sender, args[1]);
        if (profile.isEmpty()) {
            return true;
        }
        Optional<IslandProfile> island = runtime.island(profile.get());
        if (island.isEmpty() || !runtime.services().mastery().contains(args[2])) {
            runtime.send(sender, "unknown-subcommand");
            return true;
        }
        long value = amount(sender, args[4]);
        if (value < 0) {
            return true;
        }
        var mastery = runtime.services().mastery();
        var result = args[3].equalsIgnoreCase("set")
                ? mastery.setXp(island.get(), args[2], value)
                : mastery.addXp(island.get(), args[2], value);
        runtime.handleMasteryResult(Bukkit.getPlayer(args[1]), island.get(), args[2], result);
        runtime.sendRaw(sender, runtime.messages().prefix() + "&aMastery &e" + args[2] + " &ais now level &e"
                + mastery.levelOf(island.get(), args[2]));
        return true;
    }

    private boolean tokens(CommandSender sender, String[] args) {
        if (args.length < 4) {
            usage(sender);
            return true;
        }
        Optional<PlayerProfile> profile = target(sender, args[1]);
        if (profile.isEmpty()) {
            return true;
        }
        long value = amount(sender, args[3]);
        if (value < 0) {
            return true;
        }
        var tokens = runtime.services().tokens();
        var transaction = switch (args[2].toLowerCase(Locale.ROOT)) {
            case "take" -> tokens.take(profile.get(), value);
            case "set" -> tokens.set(profile.get(), value);
            default -> tokens.give(profile.get(), value);
        };
        if (!transaction.success()) {
            runtime.sendRaw(sender, runtime.messages().prefix() + "&cOperation failed: " + transaction.reason());
            return true;
        }
        runtime.fireTokenChange(profile.get().playerId(), transaction.delta(), transaction.newBalance(), "admin");
        runtime.sendRaw(sender, runtime.messages().prefix() + "&aBalance is now &e" + transaction.newBalance());
        return true;
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> options = new ArrayList<>();
        if (!sender.hasPermission(PERMISSION)) {
            return options;
        }
        if (args.length <= 1) {
            options.addAll(List.of("reload", "save", "debug", "inspect", "islandxp", "islandlevel",
                    "rolexp", "rolelevel", "masteryxp", "tokens"));
        } else if (args.length == 2) {
            Bukkit.getOnlinePlayers().forEach(player -> options.add(player.getName()));
        } else if (args.length == 3) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "rolexp", "rolelevel" -> options.addAll(runtime.services().roles().registry().ids());
                case "masteryxp" -> runtime.services().mastery().categories()
                        .forEach(category -> options.add(category.id()));
                case "tokens" -> options.addAll(List.of("give", "take", "set"));
                case "islandxp" -> options.addAll(List.of("add", "set", "remove"));
                default -> { }
            }
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
