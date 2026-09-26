package org.bukkit.entity;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.persistence.PersistentDataHolder;

public interface Entity extends PersistentDataHolder {
    UUID getUniqueId();
    Location getLocation();
    org.bukkit.World getWorld();
    EntityType getType();
    boolean isValid();
    void remove();
}
