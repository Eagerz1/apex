package com.apexmc.skyblock.core.skills;

/**
 * XP and currency payout of a single gameplay action, after every multiplier
 * has been applied.
 *
 * @param roleXp    XP for the acting role
 * @param islandXp  island XP
 * @param masteryXp mastery XP for the matching category
 * @param tokens    Sky Tokens granted (token find procs)
 */
public record SkillReward(long roleXp, long islandXp, long masteryXp, long tokens) {

    public static final SkillReward NONE = new SkillReward(0, 0, 0, 0);

    public SkillReward plus(SkillReward other) {
        return new SkillReward(roleXp + other.roleXp, islandXp + other.islandXp,
                masteryXp + other.masteryXp, tokens + other.tokens);
    }

    public boolean isEmpty() {
        return roleXp == 0 && islandXp == 0 && masteryXp == 0 && tokens == 0;
    }
}
