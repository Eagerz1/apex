import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.core.config.ConfigException;
import com.apexmc.skyblock.core.config.ProgressionConfig;
import com.apexmc.skyblock.core.data.BlockPosition;
import com.apexmc.skyblock.core.data.InMemoryRepository;
import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.data.ProfileManager;
import com.apexmc.skyblock.core.economy.DisabledEconomy;
import com.apexmc.skyblock.core.omnitool.OmniToolService;
import com.apexmc.skyblock.core.service.ProgressionServices;
import com.apexmc.skyblock.core.skills.SkillType;
import com.apexmc.skyblock.testsupport.Fixtures;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.random.RandomGenerator;

/** Manual end-to-end sanity run over the real shipped configuration. */
public final class SmokeRun {

    private static void step(String text) {
        System.out.println("  " + text);
    }

    private static void header(String text) {
        System.out.println();
        System.out.println("== " + text);
    }

    public static void main(String[] args) {
        RandomGenerator random = new java.util.Random(42);
        ProgressionConfig config = Fixtures.parsedDefaults();
        ProgressionServices services = new ProgressionServices(config, new DisabledEconomy());
        InMemoryRepository repository = new InMemoryRepository();
        ProfileManager profiles = new ProfileManager(repository, Runnable::run, config.season().seasonId(),
                error -> System.out.println("  storage error: " + error));

        header("1. New player joins");
        UUID playerId = UUID.randomUUID();
        PlayerProfile player = profiles.loadPlayer(playerId).join();
        IslandProfile island = profiles.loadIslandForOwner(playerId).join();
        player.islandId(island.islandId());
        step("season=" + player.seasonId() + " role=" + services.roles().activeRoleId(player)
                + " islandLevel=" + services.islands().levelOf(island) + " tokens=" + player.tokens());

        header("2. Chooses the farmer role");
        step(String.valueOf(services.roles().selectRole(player, "farmer", services.islands().levelOf(island))));

        header("3. Farms 2,000 wheat");
        for (int i = 0; i < 2_000; i++) {
            services.skills().handle(player, island, SkillType.FARMING, "WHEAT", 1, random);
        }
        step("islandLevel=" + services.islands().levelOf(island)
                + " islandXp=" + island.islandXp()
                + " farmerLevel=" + services.roles().levelOf(player, "farmer")
                + " farmingMastery=" + services.mastery().levelOf(island, "farming")
                + " tokens=" + player.tokens());

        header("4. Upgrades an OmniTool node");
        services.tokens().give(player, 2_000);
        OmniToolService.UpgradeContext context = new OmniToolService.UpgradeContext(
                services.roles().levelOf(player, "farmer"),
                services.islands().levelOf(island),
                unlock -> services.islands().isUnlocked(island, unlock));
        step("attempt below the role requirement -> "
                + services.omniTools().upgrade(player, "omni_hoe", "crop_multiplier", context).reason());
        services.roles().setLevel(player, "farmer", 10);
        OmniToolService.UpgradeContext ready = new OmniToolService.UpgradeContext(
                services.roles().levelOf(player, "farmer"),
                services.islands().levelOf(island),
                unlock -> services.islands().isUnlocked(island, unlock));
        step("upgrade crop_multiplier -> " + services.omniTools().upgrade(player, "omni_hoe", "crop_multiplier", ready));
        step("effect value now " + services.omniTools().effectByType(player, "omni_hoe", "fortune"));

        header("5. Island unlocks");
        services.islands().setLevel(island, 5);
        step("level 5 -> generator_tier_2 unlocked: " + services.islands().isUnlocked(island, "generator_tier_2"));
        step("level 5 -> generator_tier_3 unlocked: " + services.islands().isUnlocked(island, "generator_tier_3"));

        header("6. Generator placement, upgrade and production");
        BlockPosition position = new BlockPosition("world", 10, 64, 10);
        var placement = services.generators().place(island, player, "cobble_gen", position, 1);
        step("place -> " + placement.reason());
        services.tokens().give(player, 1_000);
        step("upgrade -> " + services.generators().upgrade(island, player, placement.state()));
        step("tick -> " + services.generators().tick(placement.state(), island, System.currentTimeMillis(), random)
                .map(production -> production.material() + " x" + production.amount()).orElse("nothing"));
        step("duplicate placement on the same block -> "
                + services.generators().place(island, player, "cobble_gen", position, 1).reason());

        header("7. Spawner stacking");
        services.islands().setLevel(island, 16);
        BlockPosition spawnerPos = new BlockPosition("world", 12, 64, 10);
        step("place -> " + services.spawners().place(island, player, "progression_spawner", spawnerPos, 1).reason());
        step("stack -> " + services.spawners().place(island, player, "progression_spawner", spawnerPos, 1).reason());
        step("stack size = " + services.spawners().at(spawnerPos).orElseThrow().stackSize());

        header("8. Companions");
        var granted = services.companions().grant(player, "rock_golem", null, services.islands().levelOf(island));
        var second = services.companions().grant(player, "rock_golem", null, services.islands().levelOf(island));
        services.tokens().give(player, 5_000);
        step("grant -> " + granted.reason() + " / " + second.reason());
        var merged = services.companions().merge(player, granted.companion().instanceId(),
                second.companion().instanceId(), config.companionMergeXpCarry());
        step("merge -> " + merged.reason() + " rarity=" + merged.companion().rarityId()
                + " remaining=" + player.companions().size());

        header("9. Restart persistence");
        profiles.flushBlocking();
        ProfileManager restarted = new ProfileManager(repository, Runnable::run, config.season().seasonId(),
                error -> System.out.println("  storage error: " + error));
        PlayerProfile reloaded = restarted.loadPlayer(playerId).join();
        IslandProfile reloadedIsland = restarted.loadIslandForOwner(playerId).join();
        step("tokens=" + reloaded.tokens() + " farmerLevel=" + services.roles().levelOf(reloaded, "farmer")
                + " islandLevel=" + services.islands().levelOf(reloadedIsland)
                + " toolNode=" + services.omniTools().nodeLevel(reloaded, "omni_hoe", "crop_multiplier")
                + " companions=" + reloaded.companions().size());

        header("10. Max level behaviour");
        services.islands().addXp(reloadedIsland, Long.MAX_VALUE);
        long cappedXp = reloadedIsland.islandXp();
        services.islands().addXp(reloadedIsland, 1_000_000);
        step("islandLevel=" + services.islands().levelOf(reloadedIsland)
                + " xp stable after extra gain: " + (cappedXp == reloadedIsland.islandXp()));

        header("11. Rapid duplicate events (10k crops in a loop)");
        long before = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            services.skills().handle(reloaded, reloadedIsland, SkillType.FARMING, "WHEAT", 64, random);
        }
        step("completed in " + ((System.nanoTime() - before) / 1_000_000) + "ms, farmer level "
                + services.roles().levelOf(reloaded, "farmer") + " (cap "
                + config.roles().requireRole("farmer").maxLevel() + ")");

        header("12. Missing Vault");
        step("economy available: " + services.economy().available()
                + ", policy: " + services.economy().policy()
                + ", quote(500) -> " + services.economy().quote(playerId, 500));

        header("13. Invalid configuration is rejected at startup");
        Map<String, Object> broken = Fixtures.defaultConfigMap();
        Fixtures.section(broken, "island", "levels").remove("7");
        try {
            ProgressionConfig.parse(Cfg.root(broken));
            step("FAILED: invalid config was accepted");
        } catch (ConfigException ex) {
            step("rejected: " + ex.getMessage());
        }

        header("14. Season rollover");
        Map<String, Object> nextSeason = Fixtures.defaultConfigMap();
        Fixtures.section(nextSeason, "season").put("season-id", "season-2");
        Fixtures.section(nextSeason, "season").put("keep-companions", true);
        ProgressionServices seasonTwo = new ProgressionServices(
                ProgressionConfig.parse(Cfg.root(nextSeason)), new DisabledEconomy());
        seasonTwo.seasons().applyTo(reloaded);
        seasonTwo.seasons().applyTo(reloadedIsland);
        step("island xp after reset=" + reloadedIsland.islandXp()
                + " farmer xp=" + reloaded.roleXp("farmer")
                + " companions kept=" + reloaded.companions().size()
                + " season=" + reloaded.seasonId());

        System.out.println();
        System.out.println("Smoke run finished without errors.");
        List.of().forEach(Object::toString);
    }
}
