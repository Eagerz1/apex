package com.apexmc.skyblock.paper.config;

import com.apexmc.skyblock.core.config.Cfg;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Converts a Bukkit {@link ConfigurationSection} into the plain nested maps the
 * dependency free core understands. This is the only place where the plugin
 * touches Bukkit's configuration API.
 */
public final class BukkitConfigAdapter {

    private BukkitConfigAdapter() {
    }

    public static Cfg toCfg(ConfigurationSection section) {
        return Cfg.root(toMap(section));
    }

    public static Map<String, Object> toMap(ConfigurationSection section) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (section == null) {
            return map;
        }
        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) {
                map.put(key, toMap(section.getConfigurationSection(key)));
            } else {
                map.put(key, normalise(section.get(key)));
            }
        }
        return map;
    }

    private static Object normalise(Object value) {
        if (value instanceof ConfigurationSection nested) {
            return toMap(nested);
        }
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object element : list) {
                copy.add(normalise(element));
            }
            return copy;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((key, entry) -> copy.put(String.valueOf(key), normalise(entry)));
            return copy;
        }
        return value;
    }
}
