package com.apexmc.skyblock.paper.hook;

import com.apexmc.skyblock.core.economy.EconomyBridge;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Vault bridge implemented entirely through reflection, so the plugin has no
 * compile or runtime dependency on Vault. When Vault (or its economy provider)
 * is missing, {@link #available()} returns {@code false} and every money
 * dependent feature follows the configured missing-economy policy.
 */
public final class VaultEconomyHook implements EconomyBridge {

    private final Logger logger;
    private Object economy;
    private Method hasAccount;
    private Method getBalance;
    private Method withdraw;
    private Method deposit;
    private Method format;

    public VaultEconomyHook(Logger logger) {
        this.logger = logger;
    }

    /** Attempts to bind to Vault; returns {@code true} when an economy is available. */
    public boolean setup() {
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(economyClass);
            if (provider == null || provider.getProvider() == null) {
                return false;
            }
            economy = provider.getProvider();
            hasAccount = economyClass.getMethod("hasAccount", OfflinePlayer.class);
            getBalance = economyClass.getMethod("getBalance", OfflinePlayer.class);
            withdraw = economyClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            deposit = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            format = economyClass.getMethod("format", double.class);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        } catch (ReflectiveOperationException ex) {
            logger.log(Level.WARNING, "Vault economy found but its API could not be bound", ex);
            economy = null;
            return false;
        }
    }

    @Override
    public boolean available() {
        return economy != null;
    }

    private OfflinePlayer offline(UUID playerId) {
        return Bukkit.getOfflinePlayer(playerId);
    }

    @Override
    public double balance(UUID playerId) {
        if (!available()) {
            return 0.0d;
        }
        try {
            Object result = getBalance.invoke(economy, offline(playerId));
            return result instanceof Number number ? number.doubleValue() : 0.0d;
        } catch (ReflectiveOperationException ex) {
            logger.log(Level.WARNING, "Vault balance lookup failed", ex);
            return 0.0d;
        }
    }

    @Override
    public boolean has(UUID playerId, double amount) {
        if (!available()) {
            return false;
        }
        try {
            Object account = hasAccount.invoke(economy, offline(playerId));
            if (account instanceof Boolean exists && !exists) {
                return false;
            }
        } catch (ReflectiveOperationException ignored) {
            // Older Vault providers may not implement hasAccount cleanly; fall back to the balance.
        }
        return balance(playerId) >= amount;
    }

    @Override
    public boolean withdraw(UUID playerId, double amount) {
        return transact(withdraw, playerId, amount);
    }

    @Override
    public boolean deposit(UUID playerId, double amount) {
        return transact(deposit, playerId, amount);
    }

    private boolean transact(Method method, UUID playerId, double amount) {
        if (!available() || amount <= 0) {
            return false;
        }
        try {
            Object response = method.invoke(economy, offline(playerId), amount);
            if (response == null) {
                return false;
            }
            Method transactionSuccess = response.getClass().getMethod("transactionSuccess");
            Object success = transactionSuccess.invoke(response);
            return success instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException ex) {
            logger.log(Level.WARNING, "Vault transaction failed", ex);
            return false;
        }
    }

    @Override
    public String format(double amount) {
        if (!available()) {
            return String.format(java.util.Locale.ROOT, "%.2f", amount);
        }
        try {
            return String.valueOf(format.invoke(economy, amount));
        } catch (ReflectiveOperationException ex) {
            return String.format(java.util.Locale.ROOT, "%.2f", amount);
        }
    }
}
