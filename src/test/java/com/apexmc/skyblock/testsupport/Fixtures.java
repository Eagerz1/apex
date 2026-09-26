package com.apexmc.skyblock.testsupport;

import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.core.config.ProgressionConfig;
import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.economy.EconomyBridge;
import com.apexmc.skyblock.core.service.ProgressionServices;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.random.RandomGenerator;

/** Shared test fixtures: the shipped configuration, profiles and fake economies. */
public final class Fixtures {

    private static Map<String, Object> cachedDefaults;

    private Fixtures() {
    }

    /** Loads the real {@code src/main/resources/config.yml} shipped with the plugin. */
    public static synchronized Map<String, Object> defaultConfigMap() {
        if (cachedDefaults == null) {
            try (InputStream stream = openDefaultConfig()) {
                cachedDefaults = SimpleYaml.load(stream);
            } catch (IOException ex) {
                throw new IllegalStateException("unable to read the default config.yml", ex);
            }
        }
        return deepCopy(cachedDefaults);
    }

    private static InputStream openDefaultConfig() throws IOException {
        InputStream resource = Fixtures.class.getClassLoader().getResourceAsStream("config.yml");
        if (resource != null) {
            return resource;
        }
        Path path = Path.of("src", "main", "resources", "config.yml");
        return Files.newInputStream(path);
    }

    public static Cfg defaultConfig() {
        return Cfg.root(defaultConfigMap());
    }

    public static ProgressionConfig parsedDefaults() {
        return ProgressionConfig.parse(defaultConfig());
    }

    public static ProgressionServices services() {
        return new ProgressionServices(parsedDefaults(), new com.apexmc.skyblock.core.economy.DisabledEconomy());
    }

    public static ProgressionServices services(EconomyBridge economy) {
        return new ProgressionServices(parsedDefaults(), economy);
    }

    public static PlayerProfile player() {
        return new PlayerProfile(UUID.randomUUID(), "season-1");
    }

    public static IslandProfile island(PlayerProfile owner) {
        IslandProfile island = new IslandProfile(UUID.randomUUID(), owner.playerId(), "season-1");
        owner.islandId(island.islandId());
        return island;
    }

    /** Deterministic generator that always rolls the supplied value. */
    public static RandomGenerator fixedRandom(double value) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                return (long) (value * Long.MAX_VALUE);
            }

            @Override
            public double nextDouble() {
                return value;
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> deepCopy(Map<String, Object> source) {
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                copy.put(entry.getKey(), deepCopy((Map<String, Object>) value));
            } else if (value instanceof java.util.List<?> list) {
                copy.put(entry.getKey(), new java.util.ArrayList<>(list));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }

    /** Navigates to a nested section of a mutable config map. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> section(Map<String, Object> root, String... path) {
        Map<String, Object> cursor = root;
        for (String key : path) {
            cursor = (Map<String, Object>) cursor.get(key);
        }
        return cursor;
    }
}
