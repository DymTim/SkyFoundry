package net.skyfoundry.core.service;

import net.skyfoundry.core.config.ConfigManager;
import net.skyfoundry.core.island.IslandRepository;

public final class IslandLocationService {

    private final IslandRepository islandRepository;
    private final ConfigManager configManager;

    public IslandLocationService(
            IslandRepository islandRepository,
            ConfigManager configManager) {
        this.islandRepository = islandRepository;
        this.configManager = configManager;
    }

    public IslandCoordinates allocateNext() {
        int slot = islandRepository.getNextSlotIndex();
        int spacing = configManager.getIslandSpacing();

        if (slot == 0) {
            return new IslandCoordinates(
                    slot,
                    0,
                    0);
        }

        int x = 0;
        int z = 0;

        int dx = 0;
        int dz = -1;

        int side = (int) Math.ceil(Math.sqrt(slot + 1));
        int searchRadius = side * side;

        for (int i = 0; i < searchRadius; i++) {

            if (i == slot) {
                return new IslandCoordinates(
                        slot,
                        x * spacing,
                        z * spacing);
            }

            if (x == z
                    || (x < 0 && x == -z)
                    || (x > 0 && x == 1 - z)) {

                int temporary = dx;
                dx = -dz;
                dz = temporary;
            }

            x += dx;
            z += dz;
        }

        throw new IllegalStateException(
                "Could not allocate island location.");
    }

    public record IslandCoordinates(
            int slotIndex,
            int x,
            int z) {
    }
}