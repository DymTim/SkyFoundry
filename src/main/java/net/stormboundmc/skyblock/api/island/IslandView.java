package net.stormboundmc.skyblock.api.island;

import java.util.UUID;
import java.util.Map;

public record IslandView(
                long id,
                UUID ownerUuid,
                String worldName,
                int centerX,
                int centerZ,
                int size,
                int memberLimit,
                int memberCount,
                long createdAt,
                Map<String, Integer> dimensionSizes) {
}
