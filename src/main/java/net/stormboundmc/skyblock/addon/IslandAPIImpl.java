package net.stormboundmc.skyblock.addon;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import net.stormboundmc.skyblock.api.island.IslandAPI;
import net.stormboundmc.skyblock.api.island.IslandView;
import net.stormboundmc.skyblock.api.island.MemberRole;
import net.stormboundmc.skyblock.island.Island;
import net.stormboundmc.skyblock.island.IslandManager;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class IslandAPIImpl implements IslandAPI {

    private final IslandManager islandManager;

    public IslandAPIImpl(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @Override
    public Optional<IslandView> getIsland(UUID playerUuid) {
        requirePrimaryThread();
        return islandManager.getIsland(playerUuid).map(this::toView);
    }

    @Override
    public Optional<IslandView> getOwnedIsland(UUID ownerUuid) {
        requirePrimaryThread();
        return islandManager.getOwnedIsland(ownerUuid).map(this::toView);
    }

    @Override
    public Optional<IslandView> getIslandAt(Location location) {
        requirePrimaryThread();
        return islandManager.getIslandAt(location).map(this::toView);
    }

    @Override
    public Optional<MemberRole> getRole(UUID playerUuid) {
        requirePrimaryThread();
        return islandManager.getRole(playerUuid).map(role -> MemberRole.valueOf(role.name()));
    }

    @Override
    public Optional<Location> getHome(UUID playerUuid) {
        requirePrimaryThread();
        Location home = islandManager.getHome(playerUuid);
        return home == null ? Optional.empty() : Optional.of(home.clone());
    }

    @Override
    public boolean hasIsland(UUID playerUuid) {
        requirePrimaryThread();
        return islandManager.hasIsland(playerUuid);
    }

    @Override
    public boolean isMemberOf(UUID playerUuid, long islandId) {
        requirePrimaryThread();
        return islandManager.getIsland(playerUuid)
                .map(island -> island.getIslandId() == islandId)
                .orElse(false);
    }

    @Override
    public Collection<IslandView> getIslands() {
        requirePrimaryThread();
        List<IslandView> islands = islandManager.getIslands()
                .stream()
                .map(this::toView)
                .toList();

        return List.copyOf(islands);
    }

    @Override
    public int getIslandCount() {
        requirePrimaryThread();
        return islandManager.getIslandCount();
    }

    private void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(
                    "Stormbound IslandAPI must be accessed from the server thread.");
        }
    }

    private IslandView toView(Island island) {
        return new IslandView(
                island.getIslandId(),
                island.getOwnerUuid(),
                island.getHome().getWorld() == null ? "" : island.getHome().getWorld().getName(),
                island.getCenterX(),
                island.getCenterZ(),
                islandManager.getIslandSize(),
                islandManager.getMemberCount(island),
                island.getCreatedAt());
    }
}
