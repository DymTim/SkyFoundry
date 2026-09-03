package net.stormboundmc.skyblock.api.island;

import java.util.UUID;

public record IslandView(
                long id,
                UUID ownerUuid,
                String worldName,
                int centerX,
                int centerZ,
                int size,
                int memberCount,
                long createdAt) {
}
