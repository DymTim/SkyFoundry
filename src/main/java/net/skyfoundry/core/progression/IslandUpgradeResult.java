package net.skyfoundry.core.progression;

public record IslandUpgradeResult(
        int previousSize,
        int newSize,
        int maximumSize) {

    public boolean reachedMaximum() {
        return newSize >= maximumSize;
    }
}