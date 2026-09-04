package net.stormboundmc.skyblock.island;

import org.bukkit.Location;

public final class IslandDimensionData {
    private final long islandId;
    private final IslandDimension dimension;
    private boolean generated;
    private boolean unlocked;
    private int size;
    private Location home;

    public IslandDimensionData(long islandId, IslandDimension dimension, boolean generated, boolean unlocked, int size, Location home) {
        this.islandId = islandId;
        this.dimension = dimension;
        this.generated = generated;
        this.unlocked = unlocked;
        this.size = size;
        this.home = home == null ? null : home.clone();
    }

    public long getIslandId() { return islandId; }
    public IslandDimension getDimension() { return dimension; }
    public boolean isGenerated() { return generated; }
    public void setGenerated(boolean generated) { this.generated = generated; }
    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public Location getHome() { return home == null ? null : home.clone(); }
    public void setHome(Location home) { this.home = home == null ? null : home.clone(); }
}
