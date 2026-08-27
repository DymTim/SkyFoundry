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

    private final IslandSpatialIndex spatialIndex = new IslandSpatialIndex();

    private final IslandMembershipIndex membershipIndex = new IslandMembershipIndex();

    public IslandProtectionService(
            IslandRepository islandRepository) {
        this.islandRepository = islandRepository;

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

    public Optional<Island> getIslandAt(
            Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }

        return spatialIndex.findIsland(
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

        Optional<Island> island = getIslandAt(
                location);

        if (island.isEmpty()) {
            return false;
        }

        return membershipIndex.belongsToIsland(
                player.getUniqueId(),
                island.get().getId());
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