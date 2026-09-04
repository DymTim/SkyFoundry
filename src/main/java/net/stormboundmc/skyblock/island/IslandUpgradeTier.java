package net.stormboundmc.skyblock.island;

public record IslandUpgradeTier(int value, double cost) {
    public IslandUpgradeTier {
        if (value <= 0) {
            throw new IllegalArgumentException("Upgrade tier value must be greater than zero.");
        }
        if (cost < 0.0D) {
            throw new IllegalArgumentException("Upgrade tier cost cannot be negative.");
        }
    }
}
