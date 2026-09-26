package com.apexmc.skyblock.core.roles;

/**
 * A perk granted at a role level. {@code type} is free-form so owners can add
 * perks without a plugin update; the platform layer resolves known types and
 * ignores unknown ones safely.
 *
 * @param type  perk type identifier (e.g. {@code xp_bonus}, {@code token_find})
 * @param value numeric magnitude of the perk
 * @param label display label used in GUIs
 */
public record RolePerk(String type, double value, String label) {
}
