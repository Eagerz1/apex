package org.bukkit.block;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

public interface Block {
    Material getType();
    void setType(Material material);
    Location getLocation();
    World getWorld();
    int getX();
    int getY();
    int getZ();
    BlockData getBlockData();
    void setBlockData(BlockData data);
    BlockState getState();
}
