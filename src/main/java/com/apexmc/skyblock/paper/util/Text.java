package com.apexmc.skyblock.paper.util;

import org.bukkit.ChatColor;

/** Colour code helpers. All player facing strings come from configuration. */
public final class Text {

    private Text() {
    }

    public static String colour(String input) {
        return input == null ? "" : ChatColor.translateAlternateColorCodes('&', input);
    }

    public static java.util.List<String> colour(java.util.List<String> input) {
        return input.stream().map(Text::colour).toList();
    }

    /** Renders a progress bar such as {@code ||||-----}. */
    public static String bar(double fraction, int length, String filledColour, String emptyColour) {
        int filled = (int) Math.round(Math.max(0.0d, Math.min(1.0d, fraction)) * length);
        return colour(filledColour + "|".repeat(filled) + emptyColour + "|".repeat(Math.max(0, length - filled)));
    }

    public static String percent(double fraction) {
        return String.format(java.util.Locale.ROOT, "%.1f%%", Math.max(0.0d, Math.min(1.0d, fraction)) * 100.0d);
    }
}
