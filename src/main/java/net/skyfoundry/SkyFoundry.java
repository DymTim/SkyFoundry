package net.skyfoundry;

import org.bukkit.plugin.java.JavaPlugin;

public final class SkyFoundry extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getLogger().info(
                "SkyFoundry v" + getPluginMeta().getVersion() + " enabled.");

        if (getConfig().getBoolean("debug.enabled", false)) {
            getLogger().info("Debug logging is enabled.");
            getLogger().info(
                    "Running on Minecraft " + getServer().getMinecraftVersion());
            getLogger().info(
                    "Running server: " + getServer().getName());
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("SkyFoundry disabled.");
    }
}