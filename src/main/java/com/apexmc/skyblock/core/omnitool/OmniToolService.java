package com.apexmc.skyblock.core.omnitool;

import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.core.config.ConfigException;
import com.apexmc.skyblock.core.currency.TokenService;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.economy.EconomyBridge;
import com.apexmc.skyblock.core.util.Ids;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * OmniTool upgrade progression. Upgrade state lives on the player profile
 * (keyed by role id + node id), never on the item itself, which makes tool
 * duplication worthless and survives item loss.
 */
public final class OmniToolService {

    private Map<String, OmniToolDefinition> tools;
    private final TokenService tokens;
    private EconomyBridge economy;

    public OmniToolService(Map<String, OmniToolDefinition> tools, TokenService tokens, EconomyBridge economy) {
        this.tools = Map.copyOf(tools);
        this.tokens = tokens;
        this.economy = economy;
    }

    public static Map<String, OmniToolDefinition> parse(Cfg cfg) {
        Cfg section = cfg.section("omnitools");
        Map<String, OmniToolDefinition> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, Cfg> entry : section.childSections().entrySet()) {
            String id = Ids.normalize(entry.getKey());
            Cfg tool = entry.getValue();
            Map<String, UpgradeNode> nodes = new LinkedHashMap<>();
            Cfg nodesSection = tool.section("nodes");
            for (Map.Entry<String, Cfg> nodeEntry : nodesSection.childSections().entrySet()) {
                String nodeId = Ids.normalize(nodeEntry.getKey());
                Cfg node = nodeEntry.getValue();
                int maxLevel = node.getInt("max-level", 5);
                if (maxLevel < 1 || maxLevel > 1000) {
                    throw new ConfigException(node.path() + ".max-level", "must be between 1 and 1000");
                }
                nodes.put(nodeId, new UpgradeNode(
                        nodeId,
                        node.getString("display-name", nodeId),
                        node.getString("icon", "PAPER"),
                        node.getStringList("description"),
                        Ids.normalize(node.getString("effect", nodeId)),
                        maxLevel,
                        node.getDouble("value-per-level", 0.0d),
                        node.getDouble("base-value", 0.0d),
                        Math.max(0L, node.getLong("token-cost-base", 0L)),
                        Math.max(0L, node.getLong("token-cost-per-level", 0L)),
                        Math.max(0.0d, node.getDouble("money-cost-base", 0.0d)),
                        Math.max(0.0d, node.getDouble("money-cost-per-level", 0.0d)),
                        Math.max(0, node.getInt("required-role-level", 0)),
                        Math.max(0, node.getInt("role-level-per-node-level", 0)),
                        Math.max(0, node.getInt("required-island-level", 0)),
                        node.getString("required-unlock", "").trim().toLowerCase(Locale.ROOT)));
            }
            if (nodes.isEmpty()) {
                throw new ConfigException(tool.path() + ".nodes", "an omnitool must define at least one upgrade node");
            }
            parsed.put(id, new OmniToolDefinition(
                    id,
                    tool.getString("display-name", id),
                    Ids.normalize(tool.getString("role", id)),
                    com.apexmc.skyblock.core.util.Ids.key(tool.getString("material", "STONE_PICKAXE")),
                    nodes));
        }
        if (parsed.isEmpty()) {
            throw new ConfigException(section.path(), "at least one omnitool must be configured");
        }
        return parsed;
    }

    public void reload(Map<String, OmniToolDefinition> tools) {
        this.tools = Map.copyOf(tools);
    }

    public void economy(EconomyBridge economy) {
        this.economy = economy;
    }

    public Collection<OmniToolDefinition> all() {
        return tools.values();
    }

    public Optional<OmniToolDefinition> tool(String toolId) {
        return toolId == null ? Optional.empty() : Optional.ofNullable(tools.get(toolId.toLowerCase(Locale.ROOT)));
    }

    public Optional<OmniToolDefinition> toolForRole(String roleId) {
        if (roleId == null) {
            return Optional.empty();
        }
        String normalised = roleId.toLowerCase(Locale.ROOT);
        return tools.values().stream().filter(tool -> tool.roleId().equals(normalised)).findFirst();
    }

    public int nodeLevel(PlayerProfile profile, String toolId, String nodeId) {
        Integer level = profile.toolUpgrades(toolId.toLowerCase(Locale.ROOT)).get(nodeId.toLowerCase(Locale.ROOT));
        if (level == null) {
            return 0;
        }
        return Math.max(0, level);
    }

    /** Effect magnitude for gameplay listeners, clamped to the node maximum. */
    public double effectValue(PlayerProfile profile, String toolId, String nodeId) {
        Optional<OmniToolDefinition> tool = tool(toolId);
        if (tool.isEmpty()) {
            return 0.0d;
        }
        Optional<UpgradeNode> node = tool.get().node(nodeId);
        if (node.isEmpty()) {
            return 0.0d;
        }
        int level = Math.min(nodeLevel(profile, toolId, nodeId), node.get().maxLevel());
        return node.get().valueAt(level);
    }

    /** Sums every node of a tool that exposes the requested effect. */
    public double effectByType(PlayerProfile profile, String toolId, String effect) {
        Optional<OmniToolDefinition> tool = tool(toolId);
        if (tool.isEmpty()) {
            return 0.0d;
        }
        double total = 0.0d;
        for (UpgradeNode node : tool.get().nodes().values()) {
            if (node.effect().equalsIgnoreCase(effect)) {
                int level = Math.min(nodeLevel(profile, tool.get().id(), node.id()), node.maxLevel());
                if (level > 0) {
                    total += node.valueAt(level);
                }
            }
        }
        return total;
    }

    /** Context required to validate an upgrade without touching Bukkit. */
    public record UpgradeContext(int roleLevel, int islandLevel, java.util.function.Predicate<String> unlockCheck) {
    }

    /** Validates an upgrade without charging anything. */
    public UpgradeResult preview(PlayerProfile profile, String toolId, String nodeId, UpgradeContext context) {
        Optional<OmniToolDefinition> tool = tool(toolId);
        if (tool.isEmpty()) {
            return UpgradeResult.fail("unknown-tool", 0);
        }
        Optional<UpgradeNode> maybeNode = tool.get().node(nodeId);
        if (maybeNode.isEmpty()) {
            return UpgradeResult.fail("unknown-node", 0);
        }
        UpgradeNode node = maybeNode.get();
        int current = nodeLevel(profile, tool.get().id(), node.id());
        if (current >= node.maxLevel()) {
            return UpgradeResult.fail("max-level", current);
        }
        if (context.islandLevel() < node.requiredIslandLevel()) {
            return UpgradeResult.fail("island-level", current);
        }
        if (!node.requiredUnlock().isEmpty() && !context.unlockCheck().test(node.requiredUnlock())) {
            return UpgradeResult.fail("locked", current);
        }
        if (context.roleLevel() < node.requiredRoleLevelFor(current)) {
            return UpgradeResult.fail("role-level", current);
        }
        long tokenCost = node.tokenCost(current);
        double moneyCost = node.moneyCost(current);
        if (!tokens.has(profile, tokenCost)) {
            return UpgradeResult.fail("insufficient-tokens", current);
        }
        if (moneyCost > 0) {
            if (!economy.available()) {
                return UpgradeResult.fail("economy-unavailable", current);
            }
            if (!economy.has(profile.playerId(), moneyCost)) {
                return UpgradeResult.fail("insufficient-money", current);
            }
        }
        return new UpgradeResult(true, "ok", current + 1, tokenCost, moneyCost);
    }

    /** Validates, charges and applies a single node upgrade. */
    public UpgradeResult upgrade(PlayerProfile profile, String toolId, String nodeId, UpgradeContext context) {
        UpgradeResult preview = preview(profile, toolId, nodeId, context);
        if (!preview.success()) {
            return preview;
        }
        OmniToolDefinition tool = tool(toolId).orElseThrow();
        UpgradeNode node = tool.node(nodeId).orElseThrow();
        int current = nodeLevel(profile, tool.id(), node.id());

        if (preview.tokensSpent() > 0 && !tokens.take(profile, preview.tokensSpent()).success()) {
            return UpgradeResult.fail("insufficient-tokens", current);
        }
        if (preview.moneySpent() > 0 && !economy.withdraw(profile.playerId(), preview.moneySpent())) {
            // Refund tokens so a failed money withdrawal can never eat currency.
            if (preview.tokensSpent() > 0) {
                tokens.give(profile, preview.tokensSpent());
            }
            return UpgradeResult.fail("insufficient-money", current);
        }
        profile.toolUpgrades(tool.id()).put(node.id(), current + 1);
        profile.markDirty();
        return new UpgradeResult(true, "ok", current + 1, preview.tokensSpent(), preview.moneySpent());
    }

    /** Admin/season helper: drop unknown nodes and clamp levels to their maximum. */
    public void sanitize(PlayerProfile profile) {
        for (Map.Entry<String, Map<String, Integer>> entry : profile.toolUpgradesView().entrySet()) {
            Optional<OmniToolDefinition> tool = tool(entry.getKey());
            Map<String, Integer> nodes = profile.toolUpgrades(entry.getKey());
            nodes.entrySet().removeIf(node -> tool.isEmpty() || tool.get().node(node.getKey()).isEmpty());
            if (tool.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, Integer> node : nodes.entrySet()) {
                int max = tool.get().node(node.getKey()).orElseThrow().maxLevel();
                if (node.getValue() == null || node.getValue() < 0) {
                    node.setValue(0);
                } else if (node.getValue() > max) {
                    node.setValue(max);
                }
            }
        }
        profile.markDirty();
    }
}
