package com.apexmc.skyblock.core.mastery;

import java.util.List;

/**
 * A mastery node unlocked at a mastery level: an ability, recipe, mechanic,
 * passive bonus or special drop.
 *
 * @param id       unlock identifier
 * @param level    mastery level required
 * @param type     unlock type ({@code ability}, {@code recipe}, {@code passive}, {@code drop}, ...)
 * @param value    numeric magnitude (passives) or 0
 * @param label    GUI label
 * @param description GUI lore
 */
public record MasteryUnlock(String id, int level, String type, double value, String label, List<String> description) {

    public MasteryUnlock {
        description = List.copyOf(description);
    }
}
