package net.skyfoundry;

import net.skyfoundry.world.VoidChunkGenerator;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkyFoundry extends JavaPlugin {

    private World islandWorld;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getLogger().info(
                "SkyFoundry v" + getPluginMeta().getVersion() + " enabling...");

        if (!setupIslandWorld()) {
            getLogger().severe("Failed to load the SkyFoundry island world.");
            getLogger().severe("SkyFoundry will now disable.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getConfig().getBoolean("debug.enabled", false)) {
            getLogger().info("Debug logging is enabled.");
            getLogger().info(
                    "Running on Minecraft " + getServer().getMinecraftVersion());
            getLogger().info(
                    "Running server: " + getServer().getName());
            getLogger().info(
                    "Island world: " + islandWorld.getName());
        }

        getLogger().info(
                "SkyFoundry v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("SkyFoundry disabled.");
    }

    private boolean setupIslandWorld() {
        String worldName = getConfig().getString("world.name", "skyfoundry");
        boolean autoCreate = getConfig().getBoolean("world.auto-create", true);

        World existingWorld = getServer().getWorld(worldName);

        if (existingWorld != null) {
            islandWorld = existingWorld;
            getLogger().info("Loaded existing island world '" + worldName + "'.");
            return true;
        }

        if (!autoCreate) {
            getLogger().severe(
                    "Island world '" + worldName
                            + "' is not loaded and world.auto-create is disabled.");
            return false;
        }

        World.Environment environment = getConfiguredEnvironment();

        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(environment);
        creator.generator(new VoidChunkGenerator());
        creator.generateStructures(false);

        islandWorld = creator.createWorld();

        if (islandWorld == null) {
            return false;
        }

        islandWorld.setAutoSave(true);

        getLogger().info(
                "Created void island world '" + worldName + "'.");

        return true;
    }

    private World.Environment getConfiguredEnvironment() {
        String configuredEnvironment = getConfig()
                .getString("world.environment", "NORMAL")
                .toUpperCase();

        try {
            return World.Environment.valueOf(configuredEnvironment);
        } catch (IllegalArgumentException exception) {
            getLogger().warning(
                    "Invalid world.environment value '"
                            + configuredEnvironment
                            + "'. Using NORMAL.");

            return World.Environment.NORMAL;
        }
    }

    public World getIslandWorld() {
        return islandWorld;
    }
}