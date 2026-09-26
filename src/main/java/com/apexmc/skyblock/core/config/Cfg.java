package com.apexmc.skyblock.core.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Immutable, platform independent view over a nested configuration map.
 *
 * <p>The plugin never reads YAML directly: the Bukkit layer converts a
 * {@code ConfigurationSection} into nested {@link Map} instances and hands it to
 * this class. That keeps every balance value out of Java and makes the whole
 * configuration stack unit testable without a running server.</p>
 */
public final class Cfg {

    private final String path;
    private final Map<String, Object> data;

    private Cfg(String path, Map<String, Object> data) {
        this.path = path;
        this.data = data;
    }

    public static Cfg root(Map<String, Object> data) {
        return new Cfg("", data == null ? new LinkedHashMap<>() : data);
    }

    public String path() {
        return path.isEmpty() ? "root" : path;
    }

    public boolean has(String key) {
        return data.containsKey(key) && data.get(key) != null;
    }

    public java.util.Set<String> keys() {
        return Collections.unmodifiableSet(data.keySet());
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    private String child(String key) {
        return path.isEmpty() ? key : path + "." + key;
    }

    private Object require(String key) {
        Object value = data.get(key);
        if (value == null) {
            throw new ConfigException(child(key), "missing required value");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public Cfg section(String key) {
        Object value = require(key);
        if (!(value instanceof Map)) {
            throw new ConfigException(child(key), "expected a section but found " + typeName(value));
        }
        return new Cfg(child(key), (Map<String, Object>) value);
    }

    @SuppressWarnings("unchecked")
    public Cfg sectionOrEmpty(String key) {
        Object value = data.get(key);
        if (value instanceof Map) {
            return new Cfg(child(key), (Map<String, Object>) value);
        }
        return new Cfg(child(key), new LinkedHashMap<>());
    }

    /** Returns every direct child section, keyed by its own name. */
    public Map<String, Cfg> childSections() {
        Map<String, Cfg> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof Map) {
                out.put(entry.getKey(), sectionOrEmpty(entry.getKey()));
            }
        }
        return out;
    }

    public String getString(String key) {
        Object value = require(key);
        return String.valueOf(value);
    }

    public String getString(String key, String def) {
        Object value = data.get(key);
        if (value == null || (value instanceof Map<?, ?> map && map.isEmpty())) {
            return def;
        }
        return String.valueOf(value);
    }

    public int getInt(String key) {
        return (int) getLong(key);
    }

    public int getInt(String key, int def) {
        return data.containsKey(key) ? getInt(key) : def;
    }

    public long getLong(String key) {
        Object value = require(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            throw new ConfigException(child(key), "expected a whole number but found '" + value + "'");
        }
    }

    public long getLong(String key, long def) {
        return data.containsKey(key) ? getLong(key) : def;
    }

    public double getDouble(String key) {
        Object value = require(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            throw new ConfigException(child(key), "expected a number but found '" + value + "'");
        }
    }

    public double getDouble(String key, double def) {
        return data.containsKey(key) ? getDouble(key) : def;
    }

    public boolean getBoolean(String key, boolean def) {
        Object value = data.get(key);
        if (value == null) {
            return def;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String raw = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        if (raw.equals("true") || raw.equals("yes")) {
            return true;
        }
        if (raw.equals("false") || raw.equals("no")) {
            return false;
        }
        throw new ConfigException(child(key), "expected true/false but found '" + value + "'");
    }

    public List<String> getStringList(String key) {
        Object value = data.get(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new ConfigException(child(key), "expected a list but found " + typeName(value));
        }
        List<String> out = new ArrayList<>(list.size());
        for (Object element : list) {
            out.add(String.valueOf(element));
        }
        return List.copyOf(out);
    }

    /** Reads a {@code name: number} map, validating every entry. */
    public Map<String, Double> getNumberMap(String key) {
        Cfg section = sectionOrEmpty(key);
        Map<String, Double> out = new LinkedHashMap<>();
        for (String childKey : section.keys()) {
            out.put(childKey.toUpperCase(Locale.ROOT), section.getDouble(childKey));
        }
        return Map.copyOf(out);
    }

    public void requireSection(String key) {
        section(key);
    }

    private static String typeName(Object value) {
        return value == null ? "null" : value.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return "Cfg(" + path() + ")";
    }
}
