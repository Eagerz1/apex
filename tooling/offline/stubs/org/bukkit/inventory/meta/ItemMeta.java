package org.bukkit.inventory.meta;

import java.util.List;
import org.bukkit.persistence.PersistentDataHolder;

public interface ItemMeta extends PersistentDataHolder, Cloneable {
    void setDisplayName(String name);
    String getDisplayName();
    boolean hasDisplayName();
    void setLore(List<String> lore);
    List<String> getLore();
    void addItemFlags(org.bukkit.inventory.ItemFlag... flags);
    void setUnbreakable(boolean unbreakable);
}
