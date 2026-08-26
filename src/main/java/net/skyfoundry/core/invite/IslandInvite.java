package net.skyfoundry.core.invite;

import java.util.UUID;

public final class IslandInvite {

    private final long islandId;
    private final UUID inviterUuid;
    private final UUID invitedUuid;

    private final long createdAt;
    private final long expiresAt;

    public IslandInvite(
            long islandId,
            UUID inviterUuid,
            UUID invitedUuid,
            long createdAt,
            long expiresAt) {
        this.islandId = islandId;
        this.inviterUuid = inviterUuid;
        this.invitedUuid = invitedUuid;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public long getIslandId() {
        return islandId;
    }

    public UUID getInviterUuid() {
        return inviterUuid;
    }

    public UUID getInvitedUuid() {
        return invitedUuid;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAt;
    }
}