package net.skyfoundry.core.protection;

import net.skyfoundry.core.island.Island;
import net.skyfoundry.core.island.IslandRepository;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class IslandProtectionService {

    public static final String BYPASS_PERMISSION = "skyfoundry.admin.bypass";

    private final IslandRepository islandRepository;

    public IslandProtectionService(
            IslandRepository islandRepository) {
        this.islandRepository = islandRepository;
    }

    public boolean hasBypass(Player player) {
        return player.hasPermission(
                BYPASS_PERMISSION);
    }

    public Optional<Island> getIslandAt(
            Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }

        return islandRepository.findIslandAt(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockZ());
    }

    public boolean canModify(
            Player player,
            Location location) {
        if (hasBypass(player)) {
            return true;
        }

        Optional<Island> targetIsland = getIslandAt(location);

        if (targetIsland.isEmpty()) {
            return false;
        }

        Optional<Island> playerIsland = islandRepository.findByMember(
                player.getUniqueId());

        if (playerIsland.isEmpty()) {
            return false;
        }

        return playerIsland
                .get()
                .getId() == targetIsland
                        .get()
                        .getId();
    }

    public boolean canInteract(
            Player player,
            Location location) {
        return canModify(
                player,
                location);
    }

    public boolean isSameIsland(
            Location first,
            Location second) {
        Optional<Island> firstIsland = getIslandAt(first);

        Optional<Island> secondIsland = getIslandAt(second);

        if (firstIsland.isEmpty()
                || secondIsland.isEmpty()) {

            return false;
        }

        return firstIsland
                .get()
                .getId() == secondIsland
                        .get()
                        .getId();
    }

    public boolean isInsideAllocatedIsland(
            Location location) {
        return getIslandAt(
                location).isPresent();
    }
}