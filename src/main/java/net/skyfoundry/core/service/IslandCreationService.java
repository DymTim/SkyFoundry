package net.skyfoundry.core.service;

import net.skyfoundry.core.config.ConfigManager;
import net.skyfoundry.core.island.Island;
import net.skyfoundry.core.island.IslandRepository;
import net.skyfoundry.core.world.SkyWorldManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.UUID;

public final class IslandCreationService {

    private final ConfigManager configManager;

    private final SkyWorldManager skyWorldManager;
    private final IslandRepository islandRepository;
    private final IslandLocationService locationService;

    public IslandCreationService(
            ConfigManager configManager,
            SkyWorldManager skyWorldManager,
            IslandRepository islandRepository,
            IslandLocationService locationService) {
        this.configManager = configManager;
        this.skyWorldManager = skyWorldManager;
        this.islandRepository = islandRepository;
        this.locationService = locationService;
    }

    public Island createIsland(UUID ownerUuid) {
        IslandLocationService.IslandCoordinates coordinates = locationService.allocateNext();

        World world = skyWorldManager.getWorld();

        int centerY = configManager.getStarterPlatformY();

        Island island = islandRepository.create(
                ownerUuid,
                world.getName(),
                coordinates.x(),
                centerY,
                coordinates.z(),
                configManager.getStartingIslandSize(),
                coordinates.slotIndex());

        generateStarterIsland(island);

        return island;
    }

    public void generateStarterIsland(
            Island island) {
        if (!configManager
                .isStarterPlatformEnabled()) {

            return;
        }

        World world = skyWorldManager.getWorld();

        int radius = configManager
                .getStarterPlatformRadius();

        int y = island.getCenterY();

        for (int x = -radius; x <= radius; x++) {

            for (int z = -radius; z <= radius; z++) {

                world.getBlockAt(
                        island.getCenterX() + x,
                        y,
                        island.getCenterZ() + z).setType(
                                Material.STONE,
                                false);
            }
        }

        world.getBlockAt(
                island.getCenterX(),
                y,
                island.getCenterZ()).setType(
                        Material.BEDROCK,
                        false);
    }

    public Location getDefaultHomeLocation(
            Island island) {
        return new Location(
                skyWorldManager.getWorld(),
                island.getCenterX() + 0.5,
                island.getCenterY() + 1.0,
                island.getCenterZ() + 0.5,
                0.0f,
                0.0f);
    }
}