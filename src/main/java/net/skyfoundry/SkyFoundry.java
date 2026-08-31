package net.skyfoundry;

import net.skyfoundry.command.IslandCommand;
import net.skyfoundry.island.IslandManager;
import net.skyfoundry.storage.Database;
import net.skyfoundry.world.VoidChunkGenerator;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public final class SkyFoundry extends JavaPlugin {

    private World islandWorld;
    private Database database;
    private IslandManager islandManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        getLogger().info(
                "SkyFoundry v" + getPluginMeta().getVersion() + " enabling...");

        if (!setupIslandWorld()) {
            getLogger().severe(
                    "Failed to load the SkyFoundry island world.");
            disablePlugin();
            return;
        }

        if (!setupDatabase()) {
            getLogger().severe(
                    "Failed to initialize the SkyFoundry database.");
            disablePlugin();
            return;
        }

        if (!setupIslandManager()) {
            getLogger().severe(
                    "Failed to initialize the island manager.");
            disablePlugin();
            return;
        }

        getCommand("island").setExecutor(
                new IslandCommand(this));

        if (getConfig().getBoolean("debug.enabled", false)) {
            logDebugInformation();
        }

        getLogger().info(
                "SkyFoundry v"
                        + getPluginMeta().getVersion()
                        + " enabled.");
    }

    @Override
    public void onDisable() {
        if (database != null) {
            database.close();
        }

        getLogger().info("SkyFoundry disabled.");
    }

    private boolean setupIslandWorld() {
        String worldName = getConfig().getString(
                "world.name",
                "skyfoundry");

        boolean autoCreate = getConfig().getBoolean(
                "world.auto-create",
                true);

        World existingWorld = getServer().getWorld(worldName);

        if (existingWorld != null) {
            islandWorld = existingWorld;

            getLogger().info(
                    "Loaded existing island world '"
                            + worldName
                            + "'.");

            return true;
        }

        if (!autoCreate) {
            getLogger().severe(
                    "Island world '"
                            + worldName
                            + "' is not loaded and "
                            + "world.auto-create is disabled.");

            return false;
        }

        WorldCreator creator = new WorldCreator(worldName);

        creator.environment(getConfiguredEnvironment());
        creator.generator(new VoidChunkGenerator());
        creator.generateStructures(false);

        islandWorld = creator.createWorld();

        if (islandWorld == null) {
            return false;
        }

        islandWorld.setAutoSave(true);

        getLogger().info(
                "Created void island world '"
                        + worldName
                        + "'.");

        return true;
    }

    private World.Environment getConfiguredEnvironment() {
        String configuredEnvironment = getConfig()
                .getString("world.environment", "NORMAL")
                .toUpperCase();

        try {
            return World.Environment.valueOf(
                    configuredEnvironment);
        } catch (IllegalArgumentException exception) {
            getLogger().warning(
                    "Invalid world.environment value '"
                            + configuredEnvironment
                            + "'. Using NORMAL.");

            return World.Environment.NORMAL;
        }
    }

    private boolean setupDatabase() {
        database = new Database(this);

        try {
            database.initialize();

            getLogger().info(
                    "SQLite database initialized.");

            return true;

        } catch (SQLException exception) {
            getLogger().severe(
                    "SQLite initialization failed: "
                            + exception.getMessage());

            return false;
        }
    }

    private boolean setupIslandManager() {
        islandManager = new IslandManager(
                this,
                database,
                islandWorld);

        try {
            islandManager.loadIslands();
            return true;

        } catch (SQLException exception) {
            getLogger().severe(
                    "Failed to load islands: "
                            + exception.getMessage());

            return false;
        }
    }

    private void logDebugInformation() {
        getLogger().info("Debug logging is enabled.");

        getLogger().info(
                "Running on Minecraft "
                        + getServer().getMinecraftVersion());

        getLogger().info(
                "Running server: "
                        + getServer().getName());

        getLogger().info(
                "Island world: "
                        + islandWorld.getName());

        getLogger().info(
                "Loaded islands: "
                        + islandManager.getIslandCount());
    }

    private void disablePlugin() {
        getLogger().severe(
                "SkyFoundry will now disable.");

        getServer()
                .getPluginManager()
                .disablePlugin(this);
    }

    public World getIslandWorld() {
        return islandWorld;
    }

    public Database getDatabase() {
        return database;
    }

    public IslandManager getIslandManager() {
        return islandManager;
    }
}