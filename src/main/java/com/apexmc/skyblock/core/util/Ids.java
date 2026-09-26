package com.apexmc.skyblock.core.util;

import java.util.Locale;
import java.util.regex.Pattern;

/** Helpers for normalising configuration/runtime identifiers. */
public final class Ids {

    private static final Pattern VALID = Pattern.compile("[a-z0-9_]+");

    private Ids() {
    }

    /** Lower-cases and validates an identifier such as a role or mastery id. */
    public static String normalize(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("identifier must not be null");
        }
        String id = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (id.isEmpty() || !VALID.matcher(id).matches()) {
            throw new IllegalArgumentException("invalid identifier: '" + raw + "'");
        }
        return id;
    }

    /** Uppercase material / entity key normalisation (never resolves Bukkit enums here). */
    public static String key(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }
}
