package net.skyfoundry.core.service;

import net.skyfoundry.core.invite.IslandInviteManager;
import net.skyfoundry.core.island.Island;
import net.skyfoundry.core.island.IslandMember;
import net.skyfoundry.core.island.IslandRepository;
import net.skyfoundry.core.world.SkyWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class IslandDeletionService {

    private final IslandRepository islandRepository;
    private final IslandInviteManager inviteManager;
    private final IslandRegionService regionService;
    private final SkyWorldManager skyWorldManager;

    public IslandDeletionService(
            IslandRepository islandRepository,
            IslandInviteManager inviteManager,
            IslandRegionService regionService,
            SkyWorldManager skyWorldManager) {
        this.islandRepository = islandRepository;
        this.inviteManager = inviteManager;
        this.regionService = regionService;
        this.skyWorldManager = skyWorldManager;
    }

    public void deleteIsland(
            Island island) {
        teleportMembersOut(
                island);

        regionService.clearIsland(
                island,
                () -> {
                    inviteManager.clearInvitesForIsland(
                            island.getId());

                    islandRepository.deleteIsland(
                            island.getId());

                    Player owner = Bukkit.getPlayer(
                            island.getOwnerUuid());

                    if (owner != null) {
                        owner.sendMessage(
                                "§aYour island has been permanently deleted.");
                    }
                },
                throwable -> {
                    throwable.printStackTrace();

                    Player owner = Bukkit.getPlayer(
                            island.getOwnerUuid());

                    if (owner != null) {
                        owner.sendMessage(
                                "§cIsland deletion failed. The island database record was preserved.");
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
                    "§7Your island is being deleted...");
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