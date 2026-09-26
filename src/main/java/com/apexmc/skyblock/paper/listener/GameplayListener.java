package com.apexmc.skyblock.paper.listener;

import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.skills.SkillActionResult;
import com.apexmc.skyblock.core.skills.SkillDrop;
import com.apexmc.skyblock.core.skills.SkillMilestone;
import com.apexmc.skyblock.core.skills.SkillType;
import com.apexmc.skyblock.paper.SkyblockRuntime;
import com.apexmc.skyblock.paper.util.Items;
import java.util.Locale;
import java.util.Optional;
import java.util.random.RandomGenerator;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Converts gameplay into progression: mining, logging, farming (with auto
 * replant), fishing and slaying. All balance decisions live in the core
 * {@code SkillService}; this class only translates Bukkit events.
 */
public final class GameplayListener implements Listener {

    private final SkyblockRuntime runtime;
    private final ToolEffects effects;
    private final RandomGenerator random = new java.util.Random();

    public GameplayListener(SkyblockRuntime runtime, int maxAreaBlocks) {
        this.runtime = runtime;
        this.effects = new ToolEffects(runtime, maxAreaBlocks);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        String key = block.getType().name();
        SkillType type = classify(key);
        if (type == null) {
            return;
        }
        if (type == SkillType.FARMING && !isMature(block)) {
            return;
        }
        Optional<PlayerProfile> maybeProfile = runtime.profile(player);
        if (maybeProfile.isEmpty()) {
            return;
        }
        PlayerProfile profile = maybeProfile.get();
        String toolId = effects.activeToolId(profile);

        int extraBlocks = applyToolEffects(player, profile, toolId, block, type);
        award(player, type, key, 1 + extraBlocks);
        effects.applyFortune(player, new ItemStack(block.getType()),
                effects.effect(profile, toolId, "fortune"), random);
        if (type == SkillType.FARMING) {
            autoReplant(player, block);
        }
    }

    /**
     * Applies area mining, tree felling and harvest radius. Returns how many
     * extra blocks were broken so they are all rewarded in one payout.
     */
    private int applyToolEffects(Player player, PlayerProfile profile, String toolId, Block block, SkillType type) {
        if (toolId.isEmpty()) {
            return 0;
        }
        java.util.List<Block> extra = java.util.List.of();
        if (type == SkillType.MINING) {
            extra = effects.areaBlocks(block, (int) effects.effect(profile, toolId, "area_mining"));
        } else if (type == SkillType.LOGGING) {
            extra = effects.treeLogs(block, (int) effects.effect(profile, toolId, "lumber"));
        } else if (type == SkillType.FARMING) {
            extra = effects.areaBlocks(block, (int) effects.effect(profile, toolId, "crop_radius"));
        }
        for (Block additional : extra) {
            ItemStack drop = new ItemStack(additional.getType());
            additional.setType(Material.AIR);
            java.util.Map<Integer, ItemStack> overflow = player.getInventory().addItem(drop);
            if (overflow != null) {
                overflow.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
            }
        }
        return extra.size();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        String key = "COD";
        if (event.getCaught() instanceof Item item && item.getItemStack() != null) {
            key = item.getItemStack().getType().name();
        }
        award(event.getPlayer(), SkillType.FISHING, key, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null) {
            return;
        }
        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }
        award(killer, SkillType.SLAYER, entity.getType().name(), 1);
    }

    /** Finds the skill that handles a material/entity key, if any. */
    private SkillType classify(String key) {
        for (SkillType type : SkillType.values()) {
            if (runtime.services().skills().registry().skillForKey(type, key).isPresent()) {
                return type;
            }
        }
        return null;
    }

    private boolean isMature(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return true;
    }

    private void autoReplant(Player player, Block block) {
        Optional<PlayerProfile> profile = runtime.profile(player);
        if (profile.isEmpty()) {
            return;
        }
        String toolId = runtime.services().omniTools()
                .toolForRole(runtime.services().roles().activeRoleId(profile.get()))
                .map(tool -> tool.id()).orElse("");
        if (toolId.isEmpty() || runtime.services().omniTools().effectByType(profile.get(), toolId, "auto_replant") <= 0) {
            return;
        }
        Material crop = block.getType();
        BlockData data = block.getBlockData();
        if (!(data instanceof Ageable ageable)) {
            return;
        }
        ageable.setAge(0);
        block.setType(crop);
        block.setBlockData(ageable);
    }

    private void award(Player player, SkillType type, String key, int units) {
        Optional<PlayerProfile> maybeProfile = runtime.profile(player);
        if (maybeProfile.isEmpty()) {
            return;
        }
        PlayerProfile profile = maybeProfile.get();
        Optional<IslandProfile> maybeIsland = runtime.island(profile);
        if (maybeIsland.isEmpty()) {
            return;
        }
        IslandProfile island = maybeIsland.get();

        int islandBefore = runtime.services().islands().levelOf(island);
        String roleId = runtime.services().skills().registry()
                .skill(type).map(skill -> skill.roleId()).orElse("");
        int roleBefore = roleId.isEmpty() ? 0 : runtime.services().roles().levelOf(profile, roleId);
        String masteryId = runtime.services().skills().registry()
                .skill(type).map(skill -> skill.masteryCategoryId()).orElse("");
        int masteryBefore = masteryId.isEmpty() ? 0 : runtime.services().mastery().levelOf(island, masteryId);

        SkillActionResult result = runtime.services().skills()
                .handle(profile, island, type, key.toUpperCase(Locale.ROOT), units, random);
        if (!result.granted()) {
            return;
        }

        int islandAfter = runtime.services().islands().levelOf(island);
        runtime.handleIslandResult(player, island,
                new com.apexmc.skyblock.core.progression.LevelUpResult(islandBefore, islandAfter, island.islandXp(), 1));
        if (!roleId.isEmpty()) {
            int roleAfter = runtime.services().roles().levelOf(profile, roleId);
            runtime.handleRoleResult(player, roleId,
                    new com.apexmc.skyblock.core.progression.LevelUpResult(roleBefore, roleAfter, profile.roleXp(roleId), 1));
        }
        if (!masteryId.isEmpty()) {
            int masteryAfter = runtime.services().mastery().levelOf(island, masteryId);
            runtime.handleMasteryResult(player, island, masteryId,
                    new com.apexmc.skyblock.core.progression.LevelUpResult(masteryBefore, masteryAfter,
                            island.masteryXp(masteryId), 1));
        }
        if (result.reward().tokens() > 0) {
            runtime.send(player, "tokens-received", "amount", result.reward().tokens(),
                    "currency", runtime.services().tokens().displayNamePlural());
            runtime.fireTokenChange(profile.playerId(), result.reward().tokens(), profile.tokens(), "skill");
        }
        for (SkillDrop drop : result.drops()) {
            giveDrop(player, drop);
        }
        for (SkillMilestone milestone : result.milestones()) {
            runtime.sendRaw(player, runtime.messages().prefix() + "&d\u2726 Milestone: &f"
                    + SkyblockRuntime.prettify(milestone.id()));
        }
    }

    private void giveDrop(Player player, SkillDrop drop) {
        Material material = Items.material(drop.material(), null);
        if (material == null) {
            runtime.plugin().getLogger().warning("Unknown drop material '" + drop.material()
                    + "' configured for drop '" + drop.id() + "'");
            return;
        }
        ItemStack item = new ItemStack(material, Math.max(1, drop.amount()));
        java.util.Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        if (overflow != null) {
            overflow.values().forEach(left -> player.getWorld().dropItemNaturally(player.getLocation(), left));
        }
    }
}
