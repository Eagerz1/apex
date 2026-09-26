package com.apexmc.skyblock.core.currency;

import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.util.Numbers;

/**
 * Internal progression currency (named "Sky Tokens" by default, fully
 * renameable through configuration). Balances live on the player profile so
 * they are persisted with the rest of the progression data.
 */
public final class TokenService {

    private String displayName;
    private String displayNamePlural;
    private String symbol;
    private long maxBalance;

    public TokenService(String displayName, String displayNamePlural, String symbol, long maxBalance) {
        this.displayName = displayName;
        this.displayNamePlural = displayNamePlural;
        this.symbol = symbol;
        this.maxBalance = maxBalance > 0 ? maxBalance : Long.MAX_VALUE;
    }

    public void rename(String displayName, String displayNamePlural, String symbol, long maxBalance) {
        this.displayName = displayName;
        this.displayNamePlural = displayNamePlural;
        this.symbol = symbol;
        this.maxBalance = maxBalance > 0 ? maxBalance : Long.MAX_VALUE;
    }

    public String displayName() {
        return displayName;
    }

    public String displayNamePlural() {
        return displayNamePlural;
    }

    public String symbol() {
        return symbol;
    }

    public long maxBalance() {
        return maxBalance;
    }

    public long balance(PlayerProfile profile) {
        return profile.tokens();
    }

    public String format(long amount) {
        return symbol + amount + " " + (amount == 1 ? displayName : displayNamePlural);
    }

    public TokenTransaction give(PlayerProfile profile, long amount) {
        if (amount <= 0) {
            return TokenTransaction.fail("invalid-amount", profile.tokens());
        }
        long updated = Numbers.clamp(Numbers.safeAdd(profile.tokens(), amount), 0L, maxBalance);
        long delta = updated - profile.tokens();
        profile.tokens(updated);
        return TokenTransaction.ok(delta, updated);
    }

    public TokenTransaction take(PlayerProfile profile, long amount) {
        if (amount <= 0) {
            return TokenTransaction.fail("invalid-amount", profile.tokens());
        }
        if (profile.tokens() < amount) {
            return TokenTransaction.fail("insufficient-funds", profile.tokens());
        }
        long updated = profile.tokens() - amount;
        profile.tokens(updated);
        return TokenTransaction.ok(-amount, updated);
    }

    public TokenTransaction set(PlayerProfile profile, long amount) {
        if (amount < 0) {
            return TokenTransaction.fail("invalid-amount", profile.tokens());
        }
        long updated = Math.min(amount, maxBalance);
        long delta = updated - profile.tokens();
        profile.tokens(updated);
        return TokenTransaction.ok(delta, updated);
    }

    public boolean has(PlayerProfile profile, long amount) {
        return amount <= 0 || profile.tokens() >= amount;
    }

    /** Atomic transfer between two profiles; never creates or destroys tokens. */
    public TokenTransaction transfer(PlayerProfile from, PlayerProfile to, long amount) {
        if (amount <= 0) {
            return TokenTransaction.fail("invalid-amount", from.tokens());
        }
        if (from.playerId().equals(to.playerId())) {
            return TokenTransaction.fail("same-player", from.tokens());
        }
        if (from.tokens() < amount) {
            return TokenTransaction.fail("insufficient-funds", from.tokens());
        }
        long receiverBefore = to.tokens();
        TokenTransaction credited = give(to, amount);
        long actuallyCredited = to.tokens() - receiverBefore;
        if (!credited.success() || actuallyCredited != amount) {
            to.tokens(receiverBefore);
            return TokenTransaction.fail("receiver-balance-cap", from.tokens());
        }
        from.tokens(from.tokens() - amount);
        return TokenTransaction.ok(-amount, from.tokens());
    }
}
