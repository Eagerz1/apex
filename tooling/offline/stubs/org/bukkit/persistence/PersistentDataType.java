package org.bukkit.persistence;

public interface PersistentDataType<T, Z> {
    PersistentDataType<String, String> STRING = new PersistentDataType<>() { };
    PersistentDataType<Integer, Integer> INTEGER = new PersistentDataType<>() { };
}
