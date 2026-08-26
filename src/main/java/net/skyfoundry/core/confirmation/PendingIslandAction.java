package net.skyfoundry.core.confirmation;

import java.util.UUID;

public final class PendingIslandAction {

    private final IslandActionType type;
    private final long islandId;
    private final UUID actorUuid;
    private final UUID targetUuid;
    private final long expiresAt;

    public PendingIslandAction(
            IslandActionType type,
            long islandId,
            UUID actorUuid,
            UUID targetUuid,
            long expiresAt) {
        this.type = type;
        this.islandId = islandId;
        this.actorUuid = actorUuid;
        this.targetUuid = targetUuid;
        this.expiresAt = expiresAt;
    }

    public IslandActionType getType() {
        return type;
    }

    public long getIslandId() {
        return islandId;
    }

    public UUID getActorUuid() {
        return actorUuid;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }
}