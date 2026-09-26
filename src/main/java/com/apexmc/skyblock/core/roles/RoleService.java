package com.apexmc.skyblock.core.roles;

import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.progression.LevelTrack;
import com.apexmc.skyblock.core.progression.LevelUpResult;
import com.apexmc.skyblock.core.util.Numbers;
import java.util.Optional;

/**
 * Role selection and role XP. Switching roles never destroys progression: XP is
 * stored per role identifier and simply becomes inactive.
 */
public final class RoleService {

    private RoleRegistry registry;
    private double globalMultiplier = 1.0d;

    public RoleService(RoleRegistry registry) {
        this.registry = registry;
    }

    public void reload(RoleRegistry registry) {
        this.registry = registry;
    }

    public RoleRegistry registry() {
        return registry;
    }

    public void globalMultiplier(double multiplier) {
        this.globalMultiplier = Math.max(0.0d, multiplier);
    }

    public String activeRoleId(PlayerProfile profile) {
        return profile.activeRoleId()
                .filter(registry::contains)
                .orElse(registry.defaultRoleId());
    }

    public RoleDefinition activeRole(PlayerProfile profile) {
        return registry.requireRole(activeRoleId(profile));
    }

    /** Result of a role change attempt. */
    public record RoleChange(boolean success, String reason, String roleId) {
        public static RoleChange ok(String roleId) {
            return new RoleChange(true, "ok", roleId);
        }

        public static RoleChange fail(String reason) {
            return new RoleChange(false, reason, null);
        }
    }

    /**
     * Selects a role. Progression of the previous role is preserved.
     *
     * @param islandLevel current island level, used for role unlock requirements
     */
    public RoleChange selectRole(PlayerProfile profile, String roleId, int islandLevel) {
        Optional<RoleDefinition> role = registry.role(roleId);
        if (role.isEmpty()) {
            return RoleChange.fail("unknown-role");
        }
        RoleDefinition definition = role.get();
        if (islandLevel < definition.requiredIslandLevel()) {
            return RoleChange.fail("island-level-required");
        }
        if (activeRoleId(profile).equals(definition.id())) {
            return RoleChange.fail("already-active");
        }
        profile.activeRoleId(definition.id());
        return RoleChange.ok(definition.id());
    }

    public int levelOf(PlayerProfile profile, String roleId) {
        return registry.role(roleId)
                .map(role -> role.curve().levelFor(profile.roleXp(role.id())))
                .orElse(0);
    }

    public LevelTrack track(PlayerProfile profile, String roleId) {
        RoleDefinition role = registry.requireRole(roleId);
        return new LevelTrack(role.curve(), profile.roleXp(role.id()));
    }

    /** Adds XP to a specific role, applying role perks and the global multiplier. */
    public LevelUpResult addXp(PlayerProfile profile, String roleId, long amount) {
        RoleDefinition role = registry.requireRole(roleId);
        LevelTrack track = track(profile, role.id());
        double bonus = 1.0d + role.perkValue(track.level(), "xp_bonus");
        long scaled = Numbers.applyMultiplier(amount, globalMultiplier * bonus);
        LevelUpResult result = track.addXp(scaled);
        if (result.changed()) {
            profile.roleXp(role.id(), track.totalXp());
        }
        return result;
    }

    /** Adds XP to whichever role the player currently has active. */
    public LevelUpResult addActiveXp(PlayerProfile profile, long amount) {
        return addXp(profile, activeRoleId(profile), amount);
    }

    public LevelUpResult setLevel(PlayerProfile profile, String roleId, int level) {
        RoleDefinition role = registry.requireRole(roleId);
        LevelTrack track = track(profile, role.id());
        LevelUpResult result = track.setLevel(level);
        profile.roleXp(role.id(), track.totalXp());
        return result;
    }

    public LevelUpResult setXp(PlayerProfile profile, String roleId, long xp) {
        RoleDefinition role = registry.requireRole(roleId);
        LevelTrack track = track(profile, role.id());
        LevelUpResult result = track.setTotalXp(xp);
        profile.roleXp(role.id(), track.totalXp());
        return result;
    }

    public LevelUpResult removeXp(PlayerProfile profile, String roleId, long xp) {
        RoleDefinition role = registry.requireRole(roleId);
        LevelTrack track = track(profile, role.id());
        LevelUpResult result = track.removeXp(xp);
        profile.roleXp(role.id(), track.totalXp());
        return result;
    }

    /** Aggregated perk magnitude of the active role (0 when the perk is absent). */
    public double activePerkValue(PlayerProfile profile, String perkType) {
        RoleDefinition role = activeRole(profile);
        return role.perkValue(levelOf(profile, role.id()), perkType);
    }
}
