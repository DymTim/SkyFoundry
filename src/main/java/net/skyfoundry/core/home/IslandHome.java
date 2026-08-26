package net.skyfoundry.core.home;

import java.util.UUID;

public final class IslandHome {

    private final long islandId;
    private final UUID playerUuid;

    private final String worldName;

    private final double x;
    private final double y;
    private final double z;

    private final float yaw;
    private final float pitch;

    public IslandHome(
            long islandId,
            UUID playerUuid,
            String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch) {
        this.islandId = islandId;
        this.playerUuid = playerUuid;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public long getIslandId() {
        return islandId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}