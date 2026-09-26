package org.bukkit;

public enum ChatColor {
    BLACK, DARK_BLUE, GREEN, RED, GRAY, WHITE, YELLOW, AQUA;

    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        return textToTranslate;
    }

    public static String stripColor(String input) { return input; }
}
