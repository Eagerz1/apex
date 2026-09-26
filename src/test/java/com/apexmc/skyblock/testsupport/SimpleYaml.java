package com.apexmc.skyblock.testsupport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal indentation based YAML reader used by the test suite to load the
 * shipped {@code config.yml} without pulling in a YAML dependency. It supports
 * the subset the plugin configuration uses: nested maps, scalar values, quoted
 * strings, inline empty lists and block lists of scalars.
 *
 * <p>At runtime the plugin uses Bukkit's own YAML implementation; this reader
 * exists purely so the default configuration is covered by automated tests.</p>
 */
public final class SimpleYaml {

    private SimpleYaml() {
    }

    public static Map<String, Object> load(InputStream stream) throws IOException {
        List<String> lines = new ArrayList<>();
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                lines.add(line);
            }
        }
        int[] cursor = {0};
        return parseMap(lines, cursor, indentOf(lines.get(0)));
    }

    private static int indentOf(String line) {
        int indent = 0;
        while (indent < line.length() && line.charAt(indent) == ' ') {
            indent++;
        }
        return indent;
    }

    private static Map<String, Object> parseMap(List<String> lines, int[] cursor, int indent) {
        Map<String, Object> map = new LinkedHashMap<>();
        while (cursor[0] < lines.size()) {
            String line = lines.get(cursor[0]);
            int lineIndent = indentOf(line);
            if (lineIndent < indent) {
                break;
            }
            String content = line.strip();
            if (content.startsWith("- ")) {
                break;
            }
            int colon = findColon(content);
            if (colon < 0) {
                throw new IllegalStateException("malformed yaml line: " + line);
            }
            String key = unquote(content.substring(0, colon).strip());
            String value = content.substring(colon + 1).strip();
            cursor[0]++;
            if (!value.isEmpty()) {
                map.put(key, scalar(value));
                continue;
            }
            if (cursor[0] >= lines.size()) {
                map.put(key, null);
                break;
            }
            String next = lines.get(cursor[0]);
            int nextIndent = indentOf(next);
            if (nextIndent <= lineIndent) {
                // "key:" with no nested block behaves like a null value in Bukkit's YAML.
                map.put(key, null);
                continue;
            }
            if (next.strip().startsWith("- ")) {
                map.put(key, parseList(lines, cursor, nextIndent));
            } else {
                map.put(key, parseMap(lines, cursor, nextIndent));
            }
        }
        return map;
    }

    private static List<Object> parseList(List<String> lines, int[] cursor, int indent) {
        List<Object> list = new ArrayList<>();
        while (cursor[0] < lines.size()) {
            String line = lines.get(cursor[0]);
            if (indentOf(line) != indent || !line.strip().startsWith("- ")) {
                break;
            }
            list.add(scalar(line.strip().substring(2).strip()));
            cursor[0]++;
        }
        return list;
    }

    /** Finds the key/value separator, ignoring colons inside quotes. */
    private static int findColon(String content) {
        boolean inQuotes = false;
        char quote = 0;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (inQuotes) {
                if (c == quote && content.charAt(i - 1) != '\\') {
                    inQuotes = false;
                }
            } else if (c == '"' || c == '\'') {
                inQuotes = true;
                quote = c;
            } else if (c == ':') {
                return i;
            }
        }
        return -1;
    }

    private static Object scalar(String raw) {
        if (raw.equals("[]")) {
            return new ArrayList<>();
        }
        if (raw.equals("{}")) {
            return new LinkedHashMap<String, Object>();
        }
        if (raw.startsWith("\"") || raw.startsWith("'")) {
            return unquote(raw);
        }
        if (raw.equalsIgnoreCase("true") || raw.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(raw);
        }
        try {
            if (raw.contains(".") || raw.contains("e") || raw.contains("E")) {
                return Double.parseDouble(raw);
            }
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return raw;
        }
    }

    private static String unquote(String raw) {
        if (raw.length() >= 2 && ((raw.startsWith("\"") && raw.endsWith("\"")) || (raw.startsWith("'") && raw.endsWith("'")))) {
            String inner = raw.substring(1, raw.length() - 1);
            return unescape(inner);
        }
        return raw;
    }

    private static String unescape(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\\' && i + 1 < value.length()) {
                char next = value.charAt(++i);
                switch (next) {
                    case 'n' -> builder.append('\n');
                    case 't' -> builder.append('\t');
                    case '"' -> builder.append('"');
                    case '\\' -> builder.append('\\');
                    case 'u' -> {
                        String hex = value.substring(i + 1, i + 5);
                        builder.append((char) Integer.parseInt(hex, 16));
                        i += 4;
                    }
                    default -> builder.append(next);
                }
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
