package net.skyfoundry.core;

import net.skyfoundry.core.command.IslandCommand;
import net.skyfoundry.core.command.SkyFoundryCommand;
import net.skyfoundry.core.config.ConfigManager;
import net.skyfoundry.core.database.DatabaseManager;
import net.skyfoundry.core.island.IslandManager;
import net.skyfoundry.core.island.IslandRepository;
import net.skyfoundry.core.service.IslandCreationService;
import net.skyfoundry.core.service.IslandLocationService;
import net.skyfoundry.core.world.SkyWorldManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class SkyFoundry extends JavaPlugin {

    private static SkyFoundry instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;

    private SkyWorldManager skyWorldManager;

    private IslandRepository islandRepository;
    private IslandLocationService islandLocationService;
    private IslandCreationService islandCreationService;
    private IslandManager islandManager;

    @Override
    public void onLoad() {
        instance = this;

        getLogger().info("Loading SkyFoundry...");
    }

    @Override
    public void onEnable() {
        printBanner();

        try {
            initializeConfiguration();
            initializeDatabase();
            initializeWorld();
            initializeIslands();
            registerCommands();

            getLogger().info("SkyFoundry enabled successfully.");
        } catch (Exception exception) {
            getLogger().severe("SkyFoundry failed to enable.");
            exception.printStackTrace();

            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("SkyFoundry disabled.");
    }

    private void initializeConfiguration() {
        configManager = new ConfigManager(this);
        configManager.load();

        getLogger().info("Configuration loaded.");
    }

    private void initializeDatabase() {
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        getLogger().info("SQLite database initialized.");
    }

    private void initializeWorld() {
        skyWorldManager = new SkyWorldManager(this, configManager);
        skyWorldManager.loadOrCreateWorld();

        getLogger().info(
                "Island world loaded: "
                        + skyWorldManager.getWorld().getName());
    }

    private void initializeIslands() {
        islandRepository = new IslandRepository(databaseManager);

        islandLocationService = new IslandLocationService(
                islandRepository,
                configManager);

        islandCreationService = new IslandCreationService(
                configManager,
                skyWorldManager,
                islandRepository,
                islandLocationService);

        islandManager = new IslandManager(
                islandRepository,
                islandCreationService,
                skyWorldManager);

        getLogger().info("Island services initialized.");
    }

    private void registerCommands() {
        Objects.requireNonNull(
                getCommand("island"),
                "island command missing from plugin.yml").setExecutor(new IslandCommand(islandManager));

        Objects.requireNonNull(
                getCommand("skyfoundry"),
                "skyfoundry command missing from plugin.yml").setExecutor(new SkyFoundryCommand(this, islandManager));
    }

    private void printBanner() {
        getLogger().info("=================================");
        getLogger().info(" SkyFoundry");
        getLogger().info(" Version: " + getPluginMeta().getVersion());
        getLogger().info(" Minecraft: 1.21.1");
        getLogger().info("=================================");
    }

    public static SkyFoundry getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public SkyWorldManager getSkyWorldManager() {
        return skyWorldManager;
    }

    public IslandManager getIslandManager() {
        return islandManager;
    }
}