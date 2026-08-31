package net.skyfoundry;

import net.skyfoundry.addon.AddonAPIImpl;
import net.skyfoundry.addon.AddonManager;
import net.skyfoundry.addon.IslandAPIImpl;
import net.skyfoundry.api.SkyFoundryAPI;
import net.skyfoundry.command.IslandCommand;
import net.skyfoundry.island.IslandManager;
import net.skyfoundry.protection.IslandProtectionListener;
import net.skyfoundry.schematic.SchematicManager;
import net.skyfoundry.storage.Database;
import net.skyfoundry.world.VoidChunkGenerator;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;

public final class SkyFoundry extends JavaPlugin {

        private World islandWorld;
        private Database database;
        private IslandManager islandManager;
        private SchematicManager schematicManager;
        private AddonManager addonManager;

        @Override
        public void onEnable() {
                saveDefaultConfig();
                saveStarterSchematic();

                getLogger().info(
                                "SkyFoundry v"
                                                + getPluginMeta().getVersion()
                                                + " enabling...");

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

                schematicManager = new SchematicManager(this);

                addonManager = new AddonManager(this);

                SkyFoundryAPI.initialize(
                                new SkyFoundryAPI(
                                                getPluginMeta().getVersion(),
                                                new IslandAPIImpl(islandManager),
                                                new AddonAPIImpl(addonManager)));

                IslandCommand islandCommand = new IslandCommand(
                                this,
                                islandManager);

                getCommand("island").setExecutor(
                                islandCommand);

                registerListeners(
                                islandCommand);

                try {
                        addonManager.loadAndEnableAddons();
                } catch (RuntimeException exception) {
                        getLogger().severe(
                                        "Failed to initialize SkyFoundry addons: "
                                                        + exception.getMessage());
                }

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
                if (addonManager != null) {
                        addonManager.disableAddons();
                }

                SkyFoundryAPI.shutdown();

                if (database != null) {
                        database.close();
                }

                getLogger().info("SkyFoundry disabled.");
        }

        private void saveStarterSchematic() {
                String fileName = getConfig().getString(
                                "schematic.file",
                                "starter.schem");

                File schematicFile = new File(
                                getDataFolder(),
                                fileName);

                if (schematicFile.exists()) {
                        return;
                }

                try {
                        saveResource(fileName, false);

                        getLogger().info(
                                        "Saved default schematic '"
                                                        + fileName
                                                        + "'.");
                } catch (IllegalArgumentException exception) {
                        getLogger().warning(
                                        "No bundled schematic named '"
                                                        + fileName
                                                        + "' was found.");
                }
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
                                .getString(
                                                "world.environment",
                                                "NORMAL")
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
                        database.connect();
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
                getLogger().info(
                                "Debug logging is enabled.");

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

                if (addonManager != null) {
                        getLogger().info(
                                        "Discovered addons: "
                                                        + SkyFoundryAPI.get().addons().getAddons().size());
                }
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

        public SchematicManager getSchematicManager() {
                return schematicManager;
        }

        public AddonManager getAddonManager() {
                return addonManager;
        }

        private void registerListeners(
                        IslandCommand islandCommand) {
                getServer()
                                .getPluginManager()
                                .registerEvents(
                                                new IslandProtectionListener(
                                                                this,
                                                                islandManager),
                                                this);

                getServer()
                                .getPluginManager()
                                .registerEvents(
                                                islandCommand,
                                                this);
        }
}
