package net.skyfoundry.core.protection;

import net.skyfoundry.core.island.Island;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class IslandSpatialIndex {

    private final Map<String, Map<Long, List<Island>>> worlds = new HashMap<>();

    public void clear() {
        worlds.clear();
    }

    public void registerIsland(
            Island island) {
        Map<Long, List<Island>> worldIndex = worlds.computeIfAbsent(
                island.getWorldName(),
                ignored -> new HashMap<>());

        int minimumChunkX = Math.floorDiv(
                island.getMinimumX(),
                16);

        int maximumChunkX = Math.floorDiv(
                island.getMaximumX(),
                16);

        int minimumChunkZ = Math.floorDiv(
                island.getMinimumZ(),
                16);

        int maximumChunkZ = Math.floorDiv(
                island.getMaximumZ(),
                16);

        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {

            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {

                long key = chunkKey(
                        chunkX,
                        chunkZ);

                worldIndex
                        .computeIfAbsent(
                                key,
                                ignored -> new ArrayList<>())
                        .add(island);
            }
        }
    }

    public void unregisterIsland(
            Island island) {
        Map<Long, List<Island>> worldIndex = worlds.get(
                island.getWorldName());

        if (worldIndex == null) {
            return;
        }

        List<Long> emptyKeys = new ArrayList<>();

        for (Map.Entry<Long, List<Island>> entry : worldIndex.entrySet()) {

            entry.getValue().removeIf(
                    indexedIsland -> indexedIsland.getId() == island.getId());

            if (entry.getValue().isEmpty()) {
                emptyKeys.add(
                        entry.getKey());
            }
        }

        for (Long key : emptyKeys) {
            worldIndex.remove(key);
        }

        if (worldIndex.isEmpty()) {
            worlds.remove(
                    island.getWorldName());
        }
    }

    public Optional<Island> findIsland(
            String worldName,
            int x,
            int z) {
        Map<Long, List<Island>> worldIndex = worlds.get(
                worldName);

        if (worldIndex == null) {
            return Optional.empty();
        }

        int chunkX = Math.floorDiv(
                x,
                16);

        int chunkZ = Math.floorDiv(
                z,
                16);

        List<Island> candidates = worldIndex.get(
                chunkKey(
                        chunkX,
                        chunkZ));

        if (candidates == null) {
            return Optional.empty();
        }

        for (Island island : candidates) {

            if (island.contains(
                    x,
                    z)) {
                return Optional.of(
                        island);
            }
        }

        return Optional.empty();
    }

    private long chunkKey(
            int chunkX,
            int chunkZ) {
        return ((long) chunkX << 32)
                ^ (chunkZ & 0xffffffffL);
    }
}