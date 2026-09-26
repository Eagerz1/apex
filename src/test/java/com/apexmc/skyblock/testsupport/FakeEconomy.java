package com.apexmc.skyblock.testsupport;

import com.apexmc.skyblock.core.economy.EconomyBridge;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** In-memory Vault stand-in used to test money dependent flows. */
public final class FakeEconomy implements EconomyBridge {

    private final Map<UUID, Double> balances = new HashMap<>();
    private boolean available = true;
    private boolean failWithdrawals;

    public void available(boolean available) {
        this.available = available;
    }

    public void failWithdrawals(boolean failWithdrawals) {
        this.failWithdrawals = failWithdrawals;
    }

    public void set(UUID playerId, double amount) {
        balances.put(playerId, amount);
    }

    @Override
    public boolean available() {
        return available;
    }

    @Override
    public double balance(UUID playerId) {
        return balances.getOrDefault(playerId, 0.0d);
    }

    @Override
    public boolean has(UUID playerId, double amount) {
        return available && balance(playerId) >= amount;
    }

    @Override
    public boolean withdraw(UUID playerId, double amount) {
        if (!available || failWithdrawals || balance(playerId) < amount) {
            return false;
        }
        balances.put(playerId, balance(playerId) - amount);
        return true;
    }

    @Override
    public boolean deposit(UUID playerId, double amount) {
        if (!available) {
            return false;
        }
        balances.put(playerId, balance(playerId) + amount);
        return true;
    }

    @Override
    public String format(double amount) {
        return "$" + amount;
    }
}
