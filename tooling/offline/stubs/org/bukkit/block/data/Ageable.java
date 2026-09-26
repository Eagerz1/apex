package org.bukkit.block.data;

public interface Ageable extends BlockData {
    int getAge();
    void setAge(int age);
    int getMaximumAge();
}
