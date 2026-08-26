package net.skyfoundry.core.island;

import java.util.UUID;

public final class Island {

    private final long id;
    private final UUID ownerUuid;

    private final String worldName;

    private final int centerX;
    private final int centerY;
    private final int centerZ;

    private final int slotIndex;

    private int size;

    private final long createdAt;

    public Island(
            long id,
            UUID ownerUuid,
            String worldName,
            int centerX,
            int centerY,
            int centerZ,
            int size,
            int slotIndex,
            long createdAt) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.worldName = worldName;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.size = size;
        this.slotIndex = slotIndex;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public int getSize() {
        return size;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getMinimumX() {
        return centerX - (size / 2);
    }

    public int getMaximumX() {
        return centerX + (size / 2) - 1;
    }

    public int getMinimumZ() {
        return centerZ - (size / 2);
    }

    public int getMaximumZ() {
        return centerZ + (size / 2) - 1;
    }

    public boolean contains(int x, int z) {
        return x >= getMinimumX()
                && x <= getMaximumX()
                && z >= getMinimumZ()
                && z <= getMaximumZ();
    }
}