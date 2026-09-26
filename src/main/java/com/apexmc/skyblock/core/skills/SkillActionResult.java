package com.apexmc.skyblock.core.skills;

import java.util.List;
import java.util.Optional;

/**
 * Full outcome of one gameplay action, ready to be applied by the Bukkit layer.
 *
 * @param handled       whether the target was configured for this skill
 * @param blockedReason why nothing was granted ({@code island-level}, {@code unhandled} or {@code ok})
 * @param reward        XP/token payout
 * @param drops         special drops rolled for this action
 * @param milestones    milestones crossed by this action
 */
public record SkillActionResult(boolean handled,
                                String blockedReason,
                                SkillReward reward,
                                List<SkillDrop> drops,
                                List<SkillMilestone> milestones) {

    public SkillActionResult {
        drops = List.copyOf(drops);
        milestones = List.copyOf(milestones);
    }

    public static SkillActionResult unhandled() {
        return new SkillActionResult(false, "unhandled", SkillReward.NONE, List.of(), List.of());
    }

    public static SkillActionResult blocked(String reason) {
        return new SkillActionResult(true, reason, SkillReward.NONE, List.of(), List.of());
    }

    public boolean granted() {
        return handled && "ok".equals(blockedReason);
    }

    public Optional<SkillDrop> firstDrop() {
        return drops.isEmpty() ? Optional.empty() : Optional.of(drops.get(0));
    }
}
