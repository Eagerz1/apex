package com.apexmc.skyblock.core.omnitool;

/**
 * Outcome of an OmniTool upgrade attempt.
 *
 * @param success   whether the node level increased
 * @param reason    {@code ok}, {@code max-level}, {@code unknown-node}, {@code role-level},
 *                  {@code island-level}, {@code locked}, {@code insufficient-tokens},
 *                  {@code insufficient-money}, {@code economy-unavailable}
 * @param newLevel  node level after the attempt
 * @param tokensSpent tokens actually spent
 * @param moneySpent  money actually spent
 */
public record UpgradeResult(boolean success, String reason, int newLevel, long tokensSpent, double moneySpent) {

    public static UpgradeResult fail(String reason, int level) {
        return new UpgradeResult(false, reason, level, 0L, 0.0d);
    }
}
