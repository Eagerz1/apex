package org.bukkit.entity;

public interface LivingEntity extends Entity {
    double getHealth();
    void setHealth(double health);
    Player getKiller();
}
