package net.skyfoundry.core.invite;

import net.skyfoundry.core.config.ConfigManager;
import net.skyfoundry.core.island.Island;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class IslandInviteManager {

    private final ConfigManager configManager;

    private final Map<UUID, IslandInvite> invites = new HashMap<>();

    public IslandInviteManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public IslandInvite createInvite(
            Island island,
            UUID inviterUuid,
            UUID invitedUuid) {
        long createdAt = System.currentTimeMillis();

        long expiresAt = createdAt
                + (configManager.getInviteExpirationSeconds() * 1000L);

        IslandInvite invite = new IslandInvite(
                island.getId(),
                inviterUuid,
                invitedUuid,
                createdAt,
                expiresAt);

        invites.put(
                invitedUuid,
                invite);

        return invite;
    }

    public Optional<IslandInvite> getInvite(
            UUID playerUuid) {
        IslandInvite invite = invites.get(playerUuid);

        if (invite == null) {
            return Optional.empty();
        }

        if (invite.isExpired()) {
            invites.remove(playerUuid);

            return Optional.empty();
        }

        return Optional.of(invite);
    }

    public void removeInvite(UUID playerUuid) {
        invites.remove(playerUuid);
    }

    public boolean hasActiveInvite(UUID playerUuid) {
        return getInvite(playerUuid).isPresent();
    }

    public void clearInvitesForIsland(long islandId) {
        invites.entrySet().removeIf(
                entry -> entry.getValue().getIslandId() == islandId);
    }
}