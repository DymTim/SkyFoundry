package net.skyfoundry.core.island;

import java.util.UUID;

public final class IslandMember {

    private final long islandId;
    private final UUID playerUuid;
    private final IslandRole role;
    private final long joinedAt;

    public IslandMember(
            long islandId,
            UUID playerUuid,
            IslandRole role,
            long joinedAt) {
        this.islandId = islandId;
        this.playerUuid = playerUuid;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public long getIslandId() {
        return islandId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public IslandRole getRole() {
        return role;
    }

    public long getJoinedAt() {
        return joinedAt;
    }
}