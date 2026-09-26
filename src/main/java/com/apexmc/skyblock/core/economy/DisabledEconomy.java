package com.apexmc.skyblock.core.economy;

import java.util.UUID;

/** No-op economy used when Vault (or any economy provider) is missing. */
public final class DisabledEconomy implements EconomyBridge {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public double balance(UUID playerId) {
        return 0.0d;
    }

    @Override
    public boolean has(UUID playerId, double amount) {
        return false;
    }

    @Override
    public boolean withdraw(UUID playerId, double amount) {
        return false;
    }

    @Override
    public boolean deposit(UUID playerId, double amount) {
        return false;
    }

    @Override
    public String format(double amount) {
        return String.format(java.util.Locale.ROOT, "%.2f", amount);
    }
}
