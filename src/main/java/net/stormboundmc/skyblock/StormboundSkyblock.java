package net.stormboundmc.skyblock;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

import net.stormboundmc.skyblock.addon.AddonAPIImpl;
import net.stormboundmc.skyblock.addon.AddonManager;
import net.stormboundmc.skyblock.addon.IslandAPIImpl;
import net.stormboundmc.skyblock.api.StormboundAPI;
import net.stormboundmc.skyblock.command.IslandCommand;
import net.stormboundmc.skyblock.gui.IslandMenu;
import net.stormboundmc.skyblock.gui.IslandMenuListener;
import net.stormboundmc.skyblock.island.IslandManager;
import net.stormboundmc.skyblock.island.IslandSettingsManager;
import net.stormboundmc.skyblock.island.IslandVisualManager;
import net.stormboundmc.skyblock.protection.IslandProtectionListener;
import net.stormboundmc.skyblock.schematic.SchematicManager;
import net.stormboundmc.skyblock.storage.Database;
import net.stormboundmc.skyblock.world.VoidChunkGenerator;

import java.io.File;
import java.sql.SQLException;

public final class StormboundSkyblock extends JavaPlugin {

        private World islandWorld;
        private Database database;
        private IslandManager islandManager;
        private IslandSettingsManager islandSettingsManager;
        private IslandVisualManager islandVisualManager;
        private SchematicManager schematicManager;
        private AddonManager addonManager;

        @Override
        public void onEnable() {
                saveDefaultConfig();
                saveStarterSchematic();

                getLogger().info(
                                "Stormbound v"
                                                + getPluginMeta().getVersion()
                                                + " enabling...");

                if (!setupIslandWorld()) {
                        getLogger().severe(
                                        "Failed to load the Stormbound island world.");
                        disablePlugin();
                        return;
                }

                if (!setupDatabase()) {
                        getLogger().severe(
                                        "Failed to initialize the Stormbound database.");
                        disablePlugin();
                        return;
                }

                if (!setupIslandManager()) {
                        getLogger().severe(
                                        "Failed to initialize the island manager.");
                        disablePlugin();
                        return;
                }

                islandSettingsManager = new IslandSettingsManager(
                                this,
                                database,
                                islandManager);

                try {
                        islandSettingsManager.loadSettings();
                } catch (SQLException exception) {
                        getLogger().severe(
                                        "Failed to load island settings: "
                                                        + exception.getMessage());
                        disablePlugin();
                        return;
                }

                islandVisualManager = new IslandVisualManager(
                                this,
                                islandManager,
                                islandSettingsManager);

                schematicManager = new SchematicManager(this);

                addonManager = new AddonManager(this);

                StormboundAPI.initialize(
                                new StormboundAPI(
                                                getPluginMeta().getVersion(),
                                                new IslandAPIImpl(islandManager),
                                                new AddonAPIImpl(addonManager)));

                IslandMenu islandMenu = new IslandMenu(
                                this,
                                islandManager);

                IslandCommand islandCommand = new IslandCommand(
                                this,
                                islandManager,
                                islandSettingsManager,
                                islandMenu);

                getCommand("island").setExecutor(
                                islandCommand);

                registerListeners(
                                islandCommand);

                try {
                        addonManager.loadAndEnableAddons();
                } catch (RuntimeException exception) {
                        getLogger().severe(
                                        "Failed to initialize Stormbound addons: "
                                                        + exception.getMessage());
                }

                if (getConfig().getBoolean("debug.enabled", false)) {
                        logDebugInformation();
                }

                getLogger().info(
                                "Stormbound v"
                                                + getPluginMeta().getVersion()
                                                + " enabled.");
        }

        @Override
        public void onDisable() {
                if (addonManager != null) {
                        addonManager.disableAddons();
                }

                StormboundAPI.shutdown();

                if (database != null) {
                        database.close();
                }

                getLogger().info("Stormbound disabled.");
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
                                "stormbound");

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
                                                        + StormboundAPI.get().addons().getAddons().size());
                }
        }

        private void disablePlugin() {
                getLogger().severe(
                                "Stormbound will now disable.");

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

        public IslandSettingsManager getIslandSettingsManager() {
                return islandSettingsManager;
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
                                                                islandManager,
                                                                islandSettingsManager),
                                                this);

                getServer()
                                .getPluginManager()
                                .registerEvents(
                                                islandCommand,
                                                this);

                getServer()
                                .getPluginManager()
                                .registerEvents(
                                                new IslandMenuListener(
                                                                this,
                                                                islandManager,
                                                                islandSettingsManager,
                                                                islandVisualManager),
                                                this);

                getServer()
                                .getPluginManager()
                                .registerEvents(
                                                islandVisualManager,
                                                this);
        }
}
