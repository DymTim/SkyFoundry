package net.stormboundmc.skyblock.api.island;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface IslandAPI {

    Optional<IslandView> getIsland(UUID playerUuid);

    default Optional<IslandView> getIsland(Player player) {
        return getIsland(player.getUniqueId());
    }

    Optional<IslandView> getOwnedIsland(UUID ownerUuid);

    Optional<IslandView> getIslandAt(Location location);

    Optional<MemberRole> getRole(UUID playerUuid);

    Optional<Location> getHome(UUID playerUuid);

    boolean hasIsland(UUID playerUuid);

    boolean isMemberOf(UUID playerUuid, long islandId);

    Collection<IslandView> getIslands();

    int getIslandCount();
}
