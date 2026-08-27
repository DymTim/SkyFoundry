package net.skyfoundry.core.protection;

import net.skyfoundry.core.island.Island;
import net.skyfoundry.core.island.IslandMember;
import net.skyfoundry.core.island.IslandRepository;
import net.skyfoundry.core.island.IslandRepositoryListener;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

public final class IslandProtectionService
        implements IslandRepositoryListener {

    public static final String BYPASS_PERMISSION = "skyfoundry.admin.bypass";

    private final IslandRepository islandRepository;
    private final String managedWorldName;

    private final IslandSpatialIndex spatialIndex = new IslandSpatialIndex();

    private final IslandMembershipIndex membershipIndex = new IslandMembershipIndex();

    public IslandProtectionService(
            IslandRepository islandRepository,
            String managedWorldName) {
        this.islandRepository = islandRepository;

        this.managedWorldName = managedWorldName;

        rebuildIndexes();

        islandRepository.addListener(
                this);
    }

    public void rebuildIndexes() {
        spatialIndex.clear();
        membershipIndex.clear();

        for (Island island : islandRepository.findAllIslands()) {

            spatialIndex.registerIsland(
                    island);
        }

        for (IslandMember member : islandRepository.findAllMembers()) {

            membershipIndex.registerMember(
                    member);
        }
    }

    public boolean hasBypass(
            Player player) {
        return player.hasPermission(
                BYPASS_PERMISSION);
    }

    public boolean isManagedWorld(
            Location location) {
        return location.getWorld() != null
                && location
                        .getWorld()
                        .getName()
                        .equals(
                                managedWorldName);
    }

    public Optional<Island> getIslandAt(
            Location location) {
        if (!isManagedWorld(location)) {
            return Optional.empty();
        }

        return spatialIndex.findIsland(
                managedWorldName,
                location.getBlockX(),
                location.getBlockZ());
    }

    public boolean isMemberOfIsland(
            Player player,
            Island island) {
        return membershipIndex.belongsToIsland(
                player.getUniqueId(),
                island.getId());
    }

    public boolean canModify(
            Player player,
            Location location) {
        if (hasBypass(player)) {
            return true;
        }

        if (!isManagedWorld(location)) {
            return true;
        }

        Optional<Island> island = getIslandAt(
                location);

        if (island.isEmpty()) {
            return false;
        }

        return isMemberOfIsland(
                player,
                island.get());
    }

    public boolean canInteract(
            Player player,
            Location location) {
        return canModify(
                player,
                location);
    }

    public boolean canAffect(
            Location source,
            Location target) {
        boolean sourceManaged = isManagedWorld(source);

        boolean targetManaged = isManagedWorld(target);

        if (!sourceManaged
                && !targetManaged) {

            return true;
        }

        if (sourceManaged != targetManaged) {
            return false;
        }

        Optional<Island> sourceIsland = getIslandAt(
                source);

        Optional<Island> targetIsland = getIslandAt(
                target);

        if (sourceIsland.isEmpty()
                || targetIsland.isEmpty()) {

            return false;
        }

        return sourceIsland
                .get()
                .getId() == targetIsland
                        .get()
                        .getId();
    }

    public boolean isSameIsland(
            Location first,
            Location second) {
        return canAffect(
                first,
                second);
    }

    public boolean isInsideAllocatedIsland(
            Location location) {
        return getIslandAt(
                location).isPresent();
    }

    @Override
    public void onIslandCreated(
            Island island) {
        spatialIndex.registerIsland(
                island);
    }

    @Override
    public void onIslandDeleted(
            Island island) {
        spatialIndex.unregisterIsland(
                island);
    }

    @Override
    public void onMemberAdded(
            IslandMember member) {
        membershipIndex.registerMember(
                member);
    }

    @Override
    public void onMemberRemoved(
            IslandMember member) {
        membershipIndex.unregisterMember(
                member.getPlayerUuid());
    }

    @Override
    public void onMemberRoleChanged(
            IslandMember member) {
        membershipIndex.registerMember(
                member);
    }
}