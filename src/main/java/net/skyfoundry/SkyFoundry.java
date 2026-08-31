package net.skyfoundry;

import org.bukkit.plugin.java.JavaPlugin;

public final class SkyFoundry extends JavaPlugin {

    private static SkyFoundry instance;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getLogger().info("SkyFoundry v" + getPluginMeta().getVersion() + " is enabling.");
        getLogger().info("SkyFoundry has been enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SkyFoundry is shutting down.");

        instance = null;
    }

    public static SkyFoundry getInstance() {
        return instance;
    }
}

