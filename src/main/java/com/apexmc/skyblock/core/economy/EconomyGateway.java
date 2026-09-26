package com.apexmc.skyblock.core.economy;

import java.util.Locale;
import java.util.UUID;

/**
 * Wraps the {@link EconomyBridge} with the configured "no Vault installed"
 * policy so every money dependent feature behaves consistently:
 *
 * <ul>
 *   <li>{@code waive} (default) - money costs are skipped, the feature still works</li>
 *   <li>{@code block} - money dependent actions are refused with a clear reason</li>
 * </ul>
 */
public final class EconomyGateway {

    /** Behaviour when no economy provider is installed. */
    public enum Policy {
        WAIVE,
        BLOCK;

        public static Policy fromId(String id) {
            String normalised = id == null ? "waive" : id.trim().toLowerCase(Locale.ROOT);
            return switch (normalised) {
                case "block", "deny", "disable" -> BLOCK;
                case "waive", "skip", "free", "" -> WAIVE;
                default -> throw new IllegalArgumentException("unknown missing-economy behaviour: " + id);
            };
        }
    }

    /**
     * Result of quoting a money cost.
     *
     * @param reason {@code ok}, {@code economy-unavailable} or {@code insufficient-money}
     * @param amount the amount that will actually be charged (0 when waived)
     */
    public record Charge(String reason, double amount) {

        public boolean ok() {
            return "ok".equals(reason);
        }
    }

    private EconomyBridge bridge;
    private Policy policy;

    public EconomyGateway(EconomyBridge bridge, Policy policy) {
        this.bridge = bridge == null ? new DisabledEconomy() : bridge;
        this.policy = policy;
    }

    public EconomyBridge bridge() {
        return bridge;
    }

    public void bridge(EconomyBridge bridge) {
        this.bridge = bridge == null ? new DisabledEconomy() : bridge;
    }

    public void policy(Policy policy) {
        this.policy = policy;
    }

    public Policy policy() {
        return policy;
    }

    public boolean available() {
        return bridge.available();
    }

    /** Checks whether a cost can be paid, applying the missing-economy policy. */
    public Charge quote(UUID playerId, double amount) {
        if (amount <= 0) {
            return new Charge("ok", 0.0d);
        }
        if (!bridge.available()) {
            return policy == Policy.WAIVE ? new Charge("ok", 0.0d) : new Charge("economy-unavailable", 0.0d);
        }
        if (!bridge.has(playerId, amount)) {
            return new Charge("insufficient-money", amount);
        }
        return new Charge("ok", amount);
    }

    /** Withdraws a previously quoted amount; zero amounts always succeed. */
    public boolean withdraw(UUID playerId, double amount) {
        return amount <= 0 || bridge.withdraw(playerId, amount);
    }

    public boolean deposit(UUID playerId, double amount) {
        return amount > 0 && bridge.deposit(playerId, amount);
    }

    public String format(double amount) {
        return bridge.format(amount);
    }
}
