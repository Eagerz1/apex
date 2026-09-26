package org.bukkit.event.entity;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EntityDeathEvent extends Event {
    public LivingEntity getEntity() { return null; }
    public int getDroppedExp() { return 0; }
    public void setDroppedExp(int exp) { }
    @Override public HandlerList getHandlers() { return null; }
}
