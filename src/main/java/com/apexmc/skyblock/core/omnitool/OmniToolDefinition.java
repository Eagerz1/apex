package com.apexmc.skyblock.core.omnitool;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** An OmniTool: the progression-aware tool bound to a role. */
public record OmniToolDefinition(String id,
                                 String displayName,
                                 String roleId,
                                 String material,
                                 Map<String, UpgradeNode> nodes) {

    public OmniToolDefinition {
        nodes = Map.copyOf(new LinkedHashMap<>(nodes));
    }

    public Optional<UpgradeNode> node(String nodeId) {
        return nodeId == null ? Optional.empty() : Optional.ofNullable(nodes.get(nodeId.toLowerCase(java.util.Locale.ROOT)));
    }
}
