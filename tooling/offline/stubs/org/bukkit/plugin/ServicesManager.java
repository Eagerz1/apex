package org.bukkit.plugin;

public interface ServicesManager {
    <T> void register(Class<T> service, T provider, Plugin plugin, ServicePriority priority);
    <T> RegisteredServiceProvider<T> getRegistration(Class<T> service);
    void unregisterAll(Plugin plugin);
}
