package org.bukkit;

public enum Material {
    STONE, AIR, PAPER, BARRIER, SPAWNER, COBBLESTONE, BONE, DIAMOND, PLAYER_HEAD,
    GRAY_STAINED_GLASS_PANE, WHEAT, WHEAT_SEEDS, CARROTS, POTATOES, BEETROOTS, NETHER_WART,
    SUGAR_CANE, MELON, PUMPKIN, COCOA, DIAMOND_PICKAXE, FISHING_ROD, BOOK, ARROW, LIME_DYE, GRAY_DYE;

    public boolean isAir() { return this == AIR; }
    public boolean isBlock() { return true; }
    public static Material matchMaterial(String name) { return null; }
    public static Material valueOfSafe(String name) { return null; }
}
