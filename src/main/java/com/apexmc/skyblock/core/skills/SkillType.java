package com.apexmc.skyblock.core.skills;

/** The five gameplay skills shipped by default. */
public enum SkillType {

    MINING("mining"),
    FARMING("farming"),
    FISHING("fishing"),
    SLAYER("slayer"),
    LOGGING("logging");

    private final String id;

    SkillType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static SkillType fromId(String id) {
        for (SkillType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("unknown skill: " + id);
    }
}
