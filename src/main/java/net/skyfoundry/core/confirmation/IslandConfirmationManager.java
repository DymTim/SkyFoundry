package net.skyfoundry.core.confirmation;

import net.skyfoundry.core.config.ConfigManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class IslandConfirmationManager {

    private final ConfigManager configManager;

    private final Map<UUID, PendingIslandAction> actions = new HashMap<>();

    public IslandConfirmationManager(
            ConfigManager configManager) {
        this.configManager = configManager;
    }

    public PendingIslandAction create(
            IslandActionType type,
            long islandId,
            UUID actorUuid,
            UUID targetUuid) {
        long expiresAt = System.currentTimeMillis()
                + (configManager
                        .getConfirmationExpirationSeconds()
                        * 1000L);

        PendingIslandAction action = new PendingIslandAction(
                type,
                islandId,
                actorUuid,
                targetUuid,
                expiresAt);

        actions.put(
                actorUuid,
                action);

        return action;
    }

    public Optional<PendingIslandAction> get(
            UUID playerUuid) {
        PendingIslandAction action = actions.get(playerUuid);

        if (action == null) {
            return Optional.empty();
        }

        if (action.isExpired()) {
            actions.remove(playerUuid);
            return Optional.empty();
        }

        return Optional.of(action);
    }

    public boolean cancel(UUID playerUuid) {
        return actions.remove(playerUuid) != null;
    }

    public void clear(UUID playerUuid) {
        actions.remove(playerUuid);
    }
}