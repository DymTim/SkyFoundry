package net.skyfoundry.core;

import net.skyfoundry.core.command.IslandCommand;
import net.skyfoundry.core.command.SkyFoundryCommand;
import net.skyfoundry.core.config.ConfigManager;
import net.skyfoundry.core.confirmation.IslandConfirmationManager;
import net.skyfoundry.core.database.DatabaseManager;
import net.skyfoundry.core.home.IslandHomeRepository;
import net.skyfoundry.core.invite.IslandInviteManager;
import net.skyfoundry.core.island.IslandManager;
import net.skyfoundry.core.island.IslandRepository;
import net.skyfoundry.core.protection.IslandProtectionService;
import net.skyfoundry.core.protection.listener.BlockProtectionListener;
import net.skyfoundry.core.protection.listener.InteractionProtectionListener;
import net.skyfoundry.core.protection.listener.InventoryProtectionListener;
import net.skyfoundry.core.reset.PlayerResetRepository;
import net.skyfoundry.core.service.IslandCreationService;
import net.skyfoundry.core.service.IslandDeletionService;
import net.skyfoundry.core.service.IslandLocationService;
import net.skyfoundry.core.service.IslandRegionService;
import net.skyfoundry.core.service.IslandResetService;
import net.skyfoundry.core.world.SkyWorldManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class SkyFoundry extends JavaPlugin {

    private static SkyFoundry instance;

    private ConfigManager configManager;
    private DatabaseManager databaseManager;

    private SkyWorldManager skyWorldManager;

    private IslandRepository islandRepository;
    private IslandManager islandManager;

    private IslandProtectionService protectionService;

    @Override
    public void onLoad() {
        instance = this;

        getLogger().info(
                "Loading SkyFoundry..."
        );
    }

    @Override
    public void onEnable() {
        printBanner();

        try {
            initializeConfiguration();
            initializeDatabase();
            initializeWorld();
            initializeIslands();
            initializeProtection();

            registerCommands();
            registerListeners();

            getLogger().info(
                    "SkyFoundry enabled successfully."
            );

        } catch (Exception exception) {

            getLogger().severe(
                    "SkyFoundry failed to enable."
            );

            exception.printStackTrace();

            getServer()
                    .getPluginManager()
                    .disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info(
                "SkyFoundry disabled."
        );
    }

    private void initializeConfiguration() {
        configManager =
                new ConfigManager(
                        this
                );

        configManager.load();

        getLogger().info(
                "Configuration loaded."
        );
    }

    private void initializeDatabase() {
        databaseManager =
                new DatabaseManager(
                        this
                );

        databaseManager.initialize();

        getLogger().info(
                "SQLite database initialized."
        );
    }

    private void initializeWorld() {
        skyWorldManager =
                new SkyWorldManager(
                        this,
                        configManager
                );

        skyWorldManager.loadOrCreateWorld();

        getLogger().info(
                "Island world loaded: "
                        + skyWorldManager
                        .getWorld()
                        .getName()
        );
    }

    private void initializeIslands() {
        islandRepository =
                new IslandRepository(
                        databaseManager
                );

        IslandHomeRepository homeRepository =
                new IslandHomeRepository(
                        databaseManager
                );

        PlayerResetRepository resetRepository =
                new PlayerResetRepository(
                        databaseManager
                );

        IslandLocationService locationService =
                new IslandLocationService(
                        islandRepository,
                        configManager
                );

        IslandCreationService creationService =
                new IslandCreationService(
                        configManager,
                        skyWorldManager,
                        islandRepository,
                        locationService
                );

        IslandInviteManager inviteManager =
                new IslandInviteManager(
                        configManager
                );

        IslandConfirmationManager confirmationManager =
                new IslandConfirmationManager(
                        configManager
                );

        IslandRegionService regionService =
                new IslandRegionService(
                        this,
                        configManager,
                        skyWorldManager
                );

        IslandDeletionService deletionService =
                new IslandDeletionService(
                        islandRepository,
                        inviteManager,
                        regionService,
                        skyWorldManager
                );

        IslandResetService resetService =
                new IslandResetService(
                        islandRepository,
                        homeRepository,
                        resetRepository,
                        creationService,
                        regionService,
                        skyWorldManager
                );

        islandManager =
                new IslandManager(
                        configManager,
                        islandRepository,
                        homeRepository,
                        resetRepository,
                        creationService,
                        inviteManager,
                        confirmationManager,
                        regionService,
                        deletionService,
                        resetService,
                        skyWorldManager
                );

        getLogger().info(
                "Island services initialized."
        );
    }

    private void initializeProtection() {
        protectionService =
                new IslandProtectionService(
                        islandRepository
                );

        getLogger().info(
                "Island protection initialized."
        );
    }

    private void registerCommands() {
        Objects.requireNonNull(
                getCommand("island"),
                "island command missing from plugin.yml"
        ).setExecutor(
                new IslandCommand(
                        islandManager
                )
        );

        Objects.requireNonNull(
                getCommand("skyfoundry"),
                "skyfoundry command missing from plugin.yml"
        ).setExecutor(
                new SkyFoundryCommand(
                        this,
                        islandManager
                )
        );
    }

    private void registerListeners() {
        getServer()
                .getPluginManager()
                .registerEvents(
                        new BlockProtectionListener(
                                protectionService
                        ),
                        this
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        new InteractionProtectionListener(
                                protectionService
                        ),
                        this
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        new InventoryProtectionListener(
                                protectionService
                        ),
                        this
                );
    }

    private void printBanner() {
        getLogger().info(
                "================================="
        );

        getLogger().info(
                " SkyFoundry"
        );

        getLogger().info(
                " Version: "
                        + getPluginMeta()
                        .getVersion()
        );

        getLogger().info(
                " Minecraft: 1.21.1"
        );

        getLogger().info(
                "================================="
        );
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

    public IslandProtectionService getProtectionService() {
        return protectionService;
    }
}