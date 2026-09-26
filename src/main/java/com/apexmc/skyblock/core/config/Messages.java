package com.apexmc.skyblock.core.config;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Message catalogue. Every player facing string lives in configuration; the
 * Bukkit layer only performs colour translation.
 */
public final class Messages {

    private final Map<String, String> messages;
    private final String prefix;

    private Messages(Map<String, String> messages, String prefix) {
        this.messages = Map.copyOf(messages);
        this.prefix = prefix;
    }

    public static Messages fromConfig(Cfg cfg) {
        Map<String, String> parsed = new LinkedHashMap<>();
        flatten("", cfg, parsed);
        return new Messages(parsed, parsed.getOrDefault("prefix", "&b[SkyBlock] &r"));
    }

    private static void flatten(String path, Cfg cfg, Map<String, String> target) {
        for (String key : cfg.keys()) {
            String full = path.isEmpty() ? key : path + "." + key;
            Cfg child = cfg.sectionOrEmpty(key);
            if (!child.isEmpty()) {
                flatten(full, child, target);
            } else {
                target.put(full.toLowerCase(Locale.ROOT), cfg.getString(key, ""));
            }
        }
    }

    public String prefix() {
        return prefix;
    }

    /** Raw message (without prefix); returns a visible fallback when missing. */
    public String raw(String key) {
        return messages.getOrDefault(key.toLowerCase(Locale.ROOT), "&c<missing message: " + key + ">");
    }

    public boolean has(String key) {
        return messages.containsKey(key.toLowerCase(Locale.ROOT));
    }

    /** Message with {@code %placeholder%} substitution. */
    public String get(String key, Object... placeholders) {
        String message = raw(key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            message = message.replace("%" + placeholders[i] + "%", String.valueOf(placeholders[i + 1]));
        }
        return message;
    }

    public String prefixed(String key, Object... placeholders) {
        return prefix + get(key, placeholders);
    }

    public int size() {
        return messages.size();
    }
}
