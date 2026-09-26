package com.apexmc.skyblock.paper;

import com.apexmc.skyblock.core.api.SkyblockProgressionAPI;
import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.core.config.ConfigException;
import com.apexmc.skyblock.core.config.ProgressionConfig;
import com.apexmc.skyblock.core.data.InMemoryRepository;
import com.apexmc.skyblock.core.data.ProfileManager;
import com.apexmc.skyblock.core.data.ProgressionRepository;
import com.apexmc.skyblock.core.data.sql.SqlRepository;
import com.apexmc.skyblock.core.data.sql.SqlSettings;
import com.apexmc.skyblock.core.service.ProgressionServices;
import com.apexmc.skyblock.paper.api.ApiImpl;
import com.apexmc.skyblock.paper.command.AdminCommand;
import com.apexmc.skyblock.paper.command.ProgressionCommand;
import com.apexmc.skyblock.paper.config.BukkitConfigAdapter;
import com.apexmc.skyblock.paper.gui.MenuListener;
import com.apexmc.skyblock.paper.gui.Menus;
import com.apexmc.skyblock.paper.hook.VaultEconomyHook;
import com.apexmc.skyblock.paper.listener.ConnectionListener;
import com.apexmc.skyblock.paper.listener.GameplayListener;
import com.apexmc.skyblock.paper.listener.StructureListener;
import com.apexmc.skyblock.paper.task.AutoSaveTask;
import com.apexmc.skyblock.paper.task.GeneratorTask;
import com.apexmc.skyblock.paper.task.SpawnerTask;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Plugin entry point: loads and validates the configuration, opens storage,
 * wires the services and registers listeners, commands, tasks and the public
 * API. A configuration error disables the plugin instead of letting a broken
 * economy run.
 */
public final class ApexSkyblockPlugin extends JavaPlugin {

    private static final List<String> COMMANDS = List.of("progression", "islevel", "mastery", "roles",
            "omnitool", "generators", "spawners", "companions", "skytokens");

    private ExecutorService storageExecutor;
    private ProgressionRepository repository;
    private ProfileManager profiles;
    private ProgressionServices services;
    private SkyblockRuntime runtime;
    private Menus menus;
    private VaultEconomyHook vault;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ProgressionConfig config;
        try {
            config = ProgressionConfig.parse(BukkitConfigAdapter.toCfg(getConfig()));
        } catch (ConfigException ex) {
            getLogger().severe("Configuration is invalid, the plugin will not enable:");
            getLogger().severe(ex.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        config.report().warnings().forEach(warning -> getLogger().warning(warning));

        storageExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "ApexSkyBlock-Storage");
            thread.setDaemon(true);
            return thread;
        });

        vault = new VaultEconomyHook(getLogger());
        if (vault.setup()) {
            getLogger().info("Vault economy detected - money rewards and costs are active.");
        } else {
            getLogger().info("No Vault economy found - money features follow the '"
                    + config.missingEconomyPolicy() + "' policy.");
        }

        services = new ProgressionServices(config, vault);
        repository = openStorage(config);
        profiles = new ProfileManager(repository, storageExecutor, config.season().seasonId(),
                error -> getLogger().log(Level.SEVERE, "Storage operation failed", error));
        runtime = new SkyblockRuntime(this, services, profiles);
        menus = new Menus(runtime, guiSection());

        loadStructures();
        registerListeners();
        registerCommands();
        scheduleTasks(config);
        registerApi();

        getLogger().info("Apex SkyBlock Progression enabled (season " + config.season().seasonId()
                + ", " + config.islandLevels().maxLevel() + " island levels).");
    }

    private ProgressionRepository openStorage(ProgressionConfig config) {
        try {
            SqlSettings settings = SqlSettings.fromConfig(
                    BukkitConfigAdapter.toCfg(getConfig()).section("storage"),
                    getDataFolder().getAbsolutePath());
            if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
                getLogger().warning("Could not create the plugin data folder");
            }
            SqlRepository sql = new SqlRepository(settings);
            sql.initialize();
            return sql;
        } catch (RuntimeException ex) {
            getLogger().log(Level.SEVERE, "Database unavailable - falling back to in-memory storage. "
                    + "Progression will NOT be persisted until this is fixed.", ex);
            return new InMemoryRepository();
        }
    }

    private void loadStructures() {
        runAsync(() -> {
            try {
                var generators = repository.loadGenerators();
                var spawners = repository.loadSpawners();
                Bukkit.getScheduler().runTask(this, () -> {
                    services.generators().load(generators);
                    services.spawners().load(spawners);
                    getLogger().info("Loaded " + generators.size() + " generators and "
                            + spawners.size() + " spawners.");
                });
            } catch (RuntimeException ex) {
                getLogger().log(Level.SEVERE, "Failed to load placed structures", ex);
            }
        });
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new ConnectionListener(runtime), this);
        int maxAreaBlocks = BukkitConfigAdapter.toCfg(getConfig()).sectionOrEmpty("general").getInt("max-area-blocks", 27);
        Bukkit.getPluginManager().registerEvents(new GameplayListener(runtime, maxAreaBlocks), this);
        Bukkit.getPluginManager().registerEvents(new StructureListener(runtime), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(), this);
    }

    private void registerCommands() {
        AdminCommand admin = new AdminCommand(runtime);
        ProgressionCommand executor = new ProgressionCommand(runtime, menus, admin);
        for (String name : COMMANDS) {
            PluginCommand command = getCommand(name);
            if (command == null) {
                getLogger().warning("Command '" + name + "' is missing from plugin.yml");
                continue;
            }
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    private void scheduleTasks(ProgressionConfig config) {
        Cfg general = BukkitConfigAdapter.toCfg(getConfig()).sectionOrEmpty("general");
        long generatorInterval = Math.max(20L, general.getLong("generator-task-interval", 20L));
        long spawnerInterval = Math.max(20L, general.getLong("spawner-task-interval", 40L));
        long saveInterval = Math.max(30L, BukkitConfigAdapter.toCfg(getConfig())
                .sectionOrEmpty("storage").getLong("save-interval-seconds", 300L)) * 20L;

        Bukkit.getScheduler().runTaskTimer(this, new GeneratorTask(runtime), generatorInterval, generatorInterval);
        Bukkit.getScheduler().runTaskTimer(this, new SpawnerTask(runtime), spawnerInterval, spawnerInterval);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, new AutoSaveTask(runtime), saveInterval, saveInterval);
    }

    private void registerApi() {
        SkyblockProgressionAPI api = new ApiImpl(runtime);
        Bukkit.getServicesManager().register(SkyblockProgressionAPI.class, api, this, ServicePriority.Normal);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                new com.apexmc.skyblock.paper.hook.PlaceholderHook(runtime, api).register();
                getLogger().info("PlaceholderAPI expansion registered.");
            } catch (LinkageError | RuntimeException ex) {
                getLogger().log(Level.WARNING, "PlaceholderAPI hook could not be registered", ex);
            }
        }
    }

    private Cfg guiSection() {
        return BukkitConfigAdapter.toCfg(getConfig()).sectionOrEmpty("gui");
    }

    /** Reloads configuration and re-validates it; runtime state is preserved. */
    public void reloadProgression() {
        reloadConfig();
        ProgressionConfig config = ProgressionConfig.parse(BukkitConfigAdapter.toCfg(getConfig()));
        config.report().warnings().forEach(warning -> getLogger().warning(warning));
        services.reload(config);
        menus.reload(guiSection());
        getLogger().info("Configuration reloaded.");
    }

    /** Runs a task on the storage executor (never on the main thread). */
    public void runAsync(Runnable task) {
        if (storageExecutor == null || storageExecutor.isShutdown()) {
            return;
        }
        storageExecutor.execute(() -> {
            try {
                task.run();
            } catch (RuntimeException ex) {
                getLogger().log(Level.SEVERE, "Async task failed", ex);
            }
        });
    }

    public SkyblockRuntime runtime() {
        return runtime;
    }

    @Override
    public void onDisable() {
        if (runtime == null) {
            return;
        }
        Bukkit.getScheduler().cancelTasks(this);
        try {
            int saved = profiles.flushBlocking();
            repository.saveGenerators(services.generators().all());
            repository.saveSpawners(services.spawners().all());
            getLogger().info("Saved " + saved + " profiles, " + services.generators().all().size()
                    + " generators and " + services.spawners().all().size() + " spawners on shutdown.");
        } catch (RuntimeException ex) {
            getLogger().log(Level.SEVERE, "Shutdown save failed", ex);
        } finally {
            if (storageExecutor != null) {
                storageExecutor.shutdown();
                try {
                    if (!storageExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                        storageExecutor.shutdownNow();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    storageExecutor.shutdownNow();
                }
            }
            repository.close();
            Bukkit.getServicesManager().unregisterAll(this);
        }
    }
}
