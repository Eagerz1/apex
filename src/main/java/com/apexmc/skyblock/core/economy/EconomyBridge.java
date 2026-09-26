package com.apexmc.skyblock.core.economy;

import java.util.UUID;

/**
 * Money abstraction. The Bukkit layer supplies a Vault backed implementation
 * when Vault is present; otherwise {@link DisabledEconomy} is used and every
 * money dependent feature degrades cleanly instead of crashing.
 */
public interface EconomyBridge {

    boolean available();

    double balance(UUID playerId);

    boolean has(UUID playerId, double amount);

    boolean withdraw(UUID playerId, double amount);

    boolean deposit(UUID playerId, double amount);

    String format(double amount);
}
