package net.stormboundmc.skyblock.island;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;

public final class Island {

    private final long islandId;

    private UUID ownerUuid;

    private final int centerX;
    private final int centerZ;

    private final long createdAt;

    private int size;
    private int memberLimit;

    private Location home;

    public Island(
            long islandId,
            UUID ownerUuid,
            int centerX,
            int centerZ,
            Location home,
            long createdAt,
            int size,
            int memberLimit) {
        this.islandId = islandId;
        this.ownerUuid = ownerUuid;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.home = home.clone();
        this.createdAt = createdAt;
        this.size = size;
        this.memberLimit = memberLimit;
    }

    public long getIslandId() {
        return islandId;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(
            UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public long getCreatedAt() {
        return createdAt;
    }


    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public int getMemberLimit() { return memberLimit; }
    public void setMemberLimit(int memberLimit) { this.memberLimit = memberLimit; }

    public Location getHome() {
        return home.clone();
    }

    public void setHome(
            Location home) {
        this.home = home.clone();
    }

    public Location getCenter(
            World world,
            double y) {
        return new Location(
                world,
                centerX + 0.5,
                y,
                centerZ + 0.5);
    }

    public boolean contains(
            Location location,
            int size) {
        if (location.getWorld() == null
                || home.getWorld() == null
                || !location.getWorld().equals(
                        home.getWorld())) {
            return false;
        }

        int minX = centerX
                - (size / 2);

        int minZ = centerZ
                - (size / 2);

        int maxX = minX
                + size
                - 1;

        int maxZ = minZ
                + size
                - 1;

        double x = location.getX();

        double z = location.getZ();

        return x >= minX
                && x < maxX + 1.0
                && z >= minZ
                && z < maxZ + 1.0;
    }
}