package com.apexmc.skyblock.core.generators;

/**
 * Shared result type for generator and spawner upgrade/placement validation.
 *
 * @param success   whether the operation succeeded
 * @param reason    machine readable reason
 * @param newTier   resulting tier (or the unchanged tier on failure)
 * @param tokensSpent tokens charged
 * @param moneySpent  money charged
 */
public record UpgradeOutcome(boolean success, String reason, int newTier, long tokensSpent, double moneySpent) {

    public static UpgradeOutcome fail(String reason, int tier) {
        return new UpgradeOutcome(false, reason, tier, 0L, 0.0d);
    }

    public static UpgradeOutcome ok(int tier, long tokens, double money) {
        return new UpgradeOutcome(true, "ok", tier, tokens, money);
    }
}
