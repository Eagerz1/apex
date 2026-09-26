package com.apexmc.skyblock.paper.listener;

import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Applies OmniTool node effects to the world: area mining, tree felling,
 * harvest radius and fortune style extra drops.
 *
 * <p>Every effect is bounded by {@code general.max-area-blocks} so a misconfigured
 * node can never break thousands of blocks in a single event.</p>
 */
public final class ToolEffects {

    private final SkyblockRuntime runtime;
    private final int maxAreaBlocks;

    public ToolEffects(SkyblockRuntime runtime, int maxAreaBlocks) {
        this.runtime = runtime;
        this.maxAreaBlocks = Math.max(1, maxAreaBlocks);
    }

    /** Tool id bound to the player's active role, or an empty string. */
    public String activeToolId(PlayerProfile profile) {
        return runtime.services().omniTools()
                .toolForRole(runtime.services().roles().activeRoleId(profile))
                .map(tool -> tool.id())
                .orElse("");
    }

    public double effect(PlayerProfile profile, String toolId, String effect) {
        return toolId.isEmpty() ? 0.0d : runtime.services().omniTools().effectByType(profile, toolId, effect);
    }

    /**
     * Collects the extra blocks an area effect should break around the origin.
     * Only blocks of the same material are affected, which keeps the behaviour
     * predictable and avoids griefing neighbouring builds.
     */
    public List<Block> areaBlocks(Block origin, int radius) {
        List<Block> blocks = new ArrayList<>();
        if (radius <= 0) {
            return blocks;
        }
        Material type = origin.getType();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    if (blocks.size() >= maxAreaBlocks) {
                        return blocks;
                    }
                    Block block = origin.getWorld().getBlockAt(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (block.getType() == type) {
                        blocks.add(block);
                    }
                }
            }
        }
        return blocks;
    }

    /** Collects the logs of a tree above the broken log, bounded by {@code limit}. */
    public List<Block> treeLogs(Block origin, int limit) {
        List<Block> logs = new ArrayList<>();
        Material type = origin.getType();
        int max = Math.min(limit, maxAreaBlocks);
        for (int offset = 1; offset <= max; offset++) {
            Block above = origin.getWorld().getBlockAt(origin.getX(), origin.getY() + offset, origin.getZ());
            if (above.getType() != type) {
                break;
            }
            logs.add(above);
        }
        return logs;
    }

    /** Rolls fortune style bonus drops for one harvested block. */
    public void applyFortune(Player player, ItemStack baseDrop, double fortune, java.util.random.RandomGenerator random) {
        if (fortune <= 0 || baseDrop == null) {
            return;
        }
        int guaranteed = (int) Math.floor(fortune);
        double chance = fortune - guaranteed;
        int extra = guaranteed + (random.nextDouble() < chance ? 1 : 0);
        if (extra <= 0) {
            return;
        }
        ItemStack bonus = new ItemStack(baseDrop.getType(), extra * Math.max(1, baseDrop.getAmount()));
        java.util.Map<Integer, ItemStack> overflow = player.getInventory().addItem(bonus);
        if (overflow != null) {
            overflow.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
    }
}
