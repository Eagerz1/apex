package com.apexmc.skyblock.core.roles;

import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.core.config.ConfigException;
import com.apexmc.skyblock.core.progression.XpCurve;
import com.apexmc.skyblock.core.util.Ids;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Immutable registry of every configured role. */
public final class RoleRegistry {

    private final Map<String, RoleDefinition> roles;
    private final String defaultRoleId;

    private RoleRegistry(Map<String, RoleDefinition> roles, String defaultRoleId) {
        this.roles = Map.copyOf(roles);
        this.defaultRoleId = defaultRoleId;
    }

    public static RoleRegistry fromConfig(Cfg cfg) {
        Cfg rolesSection = cfg.section("roles");
        Map<String, RoleDefinition> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, Cfg> entry : rolesSection.childSections().entrySet()) {
            String id = Ids.normalize(entry.getKey());
            Cfg section = entry.getValue();
            XpCurve curve = XpCurve.fromConfig(section.section("xp-curve"));

            Map<Integer, List<RolePerk>> perks = new LinkedHashMap<>();
            Cfg perksSection = section.sectionOrEmpty("perks");
            for (Map.Entry<String, Cfg> perkEntry : perksSection.childSections().entrySet()) {
                int level;
                try {
                    level = Integer.parseInt(perkEntry.getKey().trim());
                } catch (NumberFormatException ex) {
                    throw new ConfigException(perksSection.path() + "." + perkEntry.getKey(),
                            "perk keys must be role levels (whole numbers)");
                }
                if (level < 1 || level > curve.maxLevel()) {
                    throw new ConfigException(perksSection.path() + "." + perkEntry.getKey(),
                            "perk level must be between 1 and the role max level (" + curve.maxLevel() + ")");
                }
                List<RolePerk> levelPerks = new ArrayList<>();
                for (Map.Entry<String, Cfg> single : perkEntry.getValue().childSections().entrySet()) {
                    Cfg perkCfg = single.getValue();
                    levelPerks.add(new RolePerk(
                            Ids.normalize(perkCfg.getString("type", single.getKey())),
                            perkCfg.getDouble("value", 0.0d),
                            perkCfg.getString("label", single.getKey())));
                }
                perks.put(level, List.copyOf(levelPerks));
            }

            parsed.put(id, new RoleDefinition(
                    id,
                    section.getString("display-name", id),
                    section.getString("icon", "STONE"),
                    section.getStringList("description"),
                    curve,
                    Ids.normalize(section.getString("omnitool", id)),
                    perks,
                    Math.max(0, section.getInt("required-island-level", 0))));
        }
        if (parsed.isEmpty()) {
            throw new ConfigException(rolesSection.path(), "at least one role must be configured");
        }
        String defaultRole = cfg.getString("default-role", parsed.keySet().iterator().next());
        String normalisedDefault = Ids.normalize(defaultRole);
        if (!parsed.containsKey(normalisedDefault)) {
            throw new ConfigException(cfg.path() + ".default-role", "unknown role '" + defaultRole + "'");
        }
        return new RoleRegistry(parsed, normalisedDefault);
    }

    public Optional<RoleDefinition> role(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(roles.get(id.toLowerCase(java.util.Locale.ROOT)));
    }

    public RoleDefinition requireRole(String id) {
        return role(id).orElseThrow(() -> new IllegalArgumentException("unknown role: " + id));
    }

    public Collection<RoleDefinition> all() {
        return roles.values();
    }

    public List<String> ids() {
        return List.copyOf(roles.keySet());
    }

    public String defaultRoleId() {
        return defaultRoleId;
    }

    public boolean contains(String id) {
        return role(id).isPresent();
    }
}
