package net.skyfoundry.core.island;

import net.skyfoundry.core.service.IslandCreationService;
import net.skyfoundry.core.world.SkyWorldManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

public final class IslandManager {

    private final IslandRepository islandRepository;
    private final IslandCreationService islandCreationService;
    private final SkyWorldManager skyWorldManager;

    public IslandManager(
            IslandRepository islandRepository,
            IslandCreationService islandCreationService,
            SkyWorldManager skyWorldManager) {
        this.islandRepository = islandRepository;
        this.islandCreationService = islandCreationService;
        this.skyWorldManager = skyWorldManager;
    }

    public Optional<Island> getIsland(UUID playerUuid) {
        return islandRepository.findByMember(playerUuid);
    }

    public boolean hasIsland(UUID playerUuid) {
        return getIsland(playerUuid).isPresent();
    }

    public Island createIsland(Player player) {
        if (hasIsland(player.getUniqueId())) {
            throw new IllegalStateException(
                    "Player already belongs to an island.");
        }

        return islandCreationService.createIsland(
                player.getUniqueId());
    }

    public void teleportHome(Player player) {
        Island island = getIsland(player.getUniqueId())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Player does not belong to an island."));

        Location location = islandCreationService.getHomeLocation(island);

        player.teleport(location);
    }

    public int getIslandCount() {
        return islandRepository.countIslands();
    }

    public SkyWorldManager getSkyWorldManager() {
        return skyWorldManager;
    }
}