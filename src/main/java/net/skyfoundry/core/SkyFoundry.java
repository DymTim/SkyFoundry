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
import net.skyfoundry.core.progression.BlockValueRegistry;
import net.skyfoundry.core.progression.IslandLevelService;
import net.skyfoundry.core.progression.IslandProgressionRepository;
import net.skyfoundry.core.progression.IslandUpgradeService;
import net.skyfoundry.core.progression.boundary.IslandBoundaryService;
import net.skyfoundry.core.progression.listener.DailyCombatMissionListener;
import net.skyfoundry.core.progression.listener.DailyFishingMissionListener;
import net.skyfoundry.core.progression.listener.IslandBlockProgressionListener;
import net.skyfoundry.core.progression.mission.DailyMissionGenerator;
import net.skyfoundry.core.progression.mission.DailyMissionRegistry;
import net.skyfoundry.core.progression.mission.DailyMissionService;
import net.skyfoundry.core.protection.IslandProtectionService;
import net.skyfoundry.core.protection.listener.AutomationProtectionListener;
import net.skyfoundry.core.protection.listener.BlockProtectionListener;
import net.skyfoundry.core.protection.listener.EntityProtectionListener;
import net.skyfoundry.core.protection.listener.ExplosionProtectionListener;
import net.skyfoundry.core.protection.listener.FluidProtectionListener;
import net.skyfoundry.core.protection.listener.InteractionProtectionListener;
import net.skyfoundry.core.protection.listener.InventoryProtectionListener;
import net.skyfoundry.core.protection.listener.PistonProtectionListener;
import net.skyfoundry.core.protection.listener.PlayerWorldProtectionListener;
import net.skyfoundry.core.reset.PlayerResetRepository;
import net.skyfoundry.core.service.IslandCreationService;
import net.skyfoundry.core.service.IslandDeletionService;
import net.skyfoundry.core.service.IslandLocationService;
import net.skyfoundry.core.service.IslandRegionService;
import net.skyfoundry.core.service.IslandResetService;
import net.skyfoundry.core.world.SkyWorldManager;
import org.bukkit.event.Listener;
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

    private IslandUpgradeService islandUpgradeService;
    private IslandBoundaryService islandBoundaryService;

    private IslandProgressionRepository progressionRepository;
    private IslandLevelService islandLevelService;

    private BlockValueRegistry blockValueRegistry;

    private DailyMissionRegistry dailyMissionRegistry;
    private DailyMissionService dailyMissionService;

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
            initializeProgression();

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
    }

    private void initializeDatabase() {
        databaseManager =
                new DatabaseManager(
                        this
                );

        databaseManager.initialize();
    }

    private void initializeWorld() {
        skyWorldManager =
                new SkyWorldManager(
                        this,
                        configManager
                );

        skyWorldManager.loadOrCreateWorld();
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
    }

    private void initializeProtection() {
        protectionService =
                new IslandProtectionService(
                        islandRepository,
                        skyWorldManager
                                .getWorld()
                                .getName()
                );
    }

    private void initializeProgression() {
        progressionRepository =
                new IslandProgressionRepository(
                        databaseManager
                );

        islandLevelService =
                new IslandLevelService(
                        configManager
                );

        blockValueRegistry =
                new BlockValueRegistry(
                        configManager
                );

        dailyMissionRegistry =
                new DailyMissionRegistry(
                        configManager
                );

        DailyMissionGenerator missionGenerator =
                new DailyMissionGenerator();

        dailyMissionService =
                new DailyMissionService(
                        configManager,
                        islandManager,
                        progressionRepository,
                        islandLevelService,
                        dailyMissionRegistry,
                        missionGenerator
                );

        islandUpgradeService =
                new IslandUpgradeService(
                        configManager,
                        islandManager,
                        progressionRepository,
                        protectionService
                );

        islandBoundaryService =
                new IslandBoundaryService(
                        this,
                        configManager,
                        islandManager
                );
    }

    private void registerCommands() {
        Objects.requireNonNull(
                getCommand("island")
        ).setExecutor(
                new IslandCommand(
                        islandManager,
                        islandUpgradeService,
                        islandBoundaryService,
                        progressionRepository,
                        islandLevelService,
                        dailyMissionService,
                        dailyMissionRegistry
                )
        );

        Objects.requireNonNull(
                getCommand("skyfoundry")
        ).setExecutor(
                new SkyFoundryCommand(
                        this,
                        islandManager
                )
        );
    }

    private void registerListeners() {
        registerListener(
                new BlockProtectionListener(
                        protectionService
                )
        );

        registerListener(
                new InteractionProtectionListener(
                        protectionService
                )
        );

        registerListener(
                new InventoryProtectionListener(
                        protectionService
                )
        );

        registerListener(
                new EntityProtectionListener(
                        protectionService
                )
        );

        registerListener(
                new ExplosionProtectionListener(
                        protectionService
                )
        );

        registerListener(
                new FluidProtectionListener(
                        protectionService
                )
        );

        registerListener(
                new PistonProtectionListener(
                        protectionService
                )
        );

        registerListener(
                new AutomationProtectionListener(
                        protectionService
                )
        );

        registerListener(
                new PlayerWorldProtectionListener(
                        protectionService
                )
        );

        registerListener(
                new IslandBlockProgressionListener(
                        protectionService,
                        blockValueRegistry,
                        progressionRepository,
                        dailyMissionService
                )
        );

        registerListener(
                new DailyCombatMissionListener(
                        protectionService,
                        dailyMissionService
                )
        );

        registerListener(
                new DailyFishingMissionListener(
                        protectionService,
                        dailyMissionService
                )
        );
    }

    private void registerListener(
            Listener listener
    ) {
        getServer()
                .getPluginManager()
                .registerEvents(
                        listener,
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