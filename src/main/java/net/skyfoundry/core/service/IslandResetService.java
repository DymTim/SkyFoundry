package net.skyfoundry.core.service;

import net.skyfoundry.core.home.IslandHomeRepository;
import net.skyfoundry.core.island.Island;
import net.skyfoundry.core.island.IslandMember;
import net.skyfoundry.core.island.IslandRepository;
import net.skyfoundry.core.reset.PlayerResetRepository;
import net.skyfoundry.core.world.SkyWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class IslandResetService {

    private final IslandRepository islandRepository;
    private final IslandHomeRepository homeRepository;
    private final PlayerResetRepository resetRepository;

    private final IslandCreationService creationService;
    private final IslandRegionService regionService;
    private final SkyWorldManager skyWorldManager;

    public IslandResetService(
            IslandRepository islandRepository,
            IslandHomeRepository homeRepository,
            PlayerResetRepository resetRepository,
            IslandCreationService creationService,
            IslandRegionService regionService,
            SkyWorldManager skyWorldManager) {
        this.islandRepository = islandRepository;
        this.homeRepository = homeRepository;
        this.resetRepository = resetRepository;
        this.creationService = creationService;
        this.regionService = regionService;
        this.skyWorldManager = skyWorldManager;
    }

    public void resetIsland(
            Island island) {
        teleportMembersOut(
                island);

        regionService.clearIsland(
                island,
                () -> {
                    homeRepository.deleteHomesForIsland(
                            island.getId());

                    creationService.generateStarterIsland(
                            island);

                    resetRepository.incrementUsedResets(
                            island.getOwnerUuid());

                    teleportMembersHome(
                            island);
                },
                throwable -> {
                    throwable.printStackTrace();

                    Player owner = Bukkit.getPlayer(
                            island.getOwnerUuid());

                    if (owner != null) {
                        owner.sendMessage(
                                "§cIsland reset failed. Your reset allowance was not consumed.");
                    }
                });
    }

    private void teleportMembersOut(
            Island island) {
        Location safeLocation = getSafeLocation();

        for (IslandMember member : islandRepository.getMembers(
                island.getId())) {

            Player player = Bukkit.getPlayer(
                    member.getPlayerUuid());

            if (player == null) {
                continue;
            }

            player.teleport(
                    safeLocation);

            player.sendMessage(
                    "§7Your island is being reset...");
        }
    }

    private void teleportMembersHome(
            Island island) {
        Location home = creationService
                .getDefaultHomeLocation(
                        island);

        for (IslandMember member : islandRepository.getMembers(
                island.getId())) {

            Player player = Bukkit.getPlayer(
                    member.getPlayerUuid());

            if (player == null) {
                continue;
            }

            player.teleport(
                    home);

            player.sendMessage(
                    "§aYour island reset is complete.");
        }
    }

    private Location getSafeLocation() {
        for (World world : Bukkit.getWorlds()) {

            if (!world.getName().equals(
                    skyWorldManager
                            .getWorld()
                            .getName())) {

                return world.getSpawnLocation();
            }
        }

        return skyWorldManager
                .getWorld()
                .getSpawnLocation();
    }
}