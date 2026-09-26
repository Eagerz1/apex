package com.apexmc.skyblock.core.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dependency-free string codec used to persist small key/value maps in a single
 * database column (for example OmniTool upgrade nodes).
 *
 * <p>Format: {@code key=value;key=value} with {@code \} escaping. Chosen over a
 * JSON library so the plugin ships with zero runtime dependencies.</p>
 */
public final class Codec {

    private Codec() {
    }

    public static String encode(Map<String, ? extends Object> map) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, ? extends Object> entry : map.entrySet()) {
            if (builder.length() > 0) {
                builder.append(';');
            }
            builder.append(escape(entry.getKey())).append('=').append(escape(String.valueOf(entry.getValue())));
        }
        return builder.toString();
    }

    public static Map<String, String> decode(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        StringBuilder token = new StringBuilder();
        String key = null;
        boolean escaped = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (escaped) {
                token.append(c);
                escaped = false;
                continue;
            }
            switch (c) {
                case '\\' -> escaped = true;
                case '=' -> {
                    if (key == null) {
                        key = token.toString();
                        token.setLength(0);
                    } else {
                        token.append(c);
                    }
                }
                case ';' -> {
                    if (key != null) {
                        out.put(key, token.toString());
                    }
                    key = null;
                    token.setLength(0);
                }
                default -> token.append(c);
            }
        }
        if (key != null) {
            out.put(key, token.toString());
        }
        return out;
    }

    public static Map<String, Integer> decodeInts(String raw) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : decode(raw).entrySet()) {
            try {
                out.put(entry.getKey(), Integer.parseInt(entry.getValue().trim()));
            } catch (NumberFormatException ignored) {
                // Corrupt value: skip the entry rather than failing the whole profile load.
            }
        }
        return out;
    }

    private static String escape(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' || c == '=' || c == ';') {
                builder.append('\\');
            }
            builder.append(c);
        }
        return builder.toString();
    }
}
