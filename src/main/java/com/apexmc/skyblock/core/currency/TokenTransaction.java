package com.apexmc.skyblock.core.currency;

/**
 * Outcome of a Sky Token operation.
 *
 * @param success    whether the balance changed
 * @param reason     machine readable reason ({@code ok}, {@code insufficient-funds}, {@code invalid-amount})
 * @param delta      applied change (may be negative)
 * @param newBalance balance after the operation
 */
public record TokenTransaction(boolean success, String reason, long delta, long newBalance) {

    public static TokenTransaction ok(long delta, long newBalance) {
        return new TokenTransaction(true, "ok", delta, newBalance);
    }

    public static TokenTransaction fail(String reason, long balance) {
        return new TokenTransaction(false, reason, 0L, balance);
    }
}
