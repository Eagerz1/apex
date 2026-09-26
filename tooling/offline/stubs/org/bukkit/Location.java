package org.bukkit;

import org.bukkit.block.Block;

public class Location implements Cloneable {
    public Location(World world, double x, double y, double z) { }
    public World getWorld() { return null; }
    public int getBlockX() { return 0; }
    public int getBlockY() { return 0; }
    public int getBlockZ() { return 0; }
    public Location add(double x, double y, double z) { return this; }
    public Block getBlock() { return null; }
    @Override public Location clone() { return this; }
}
