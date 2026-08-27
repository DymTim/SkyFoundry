package net.skyfoundry.core.protection;

import net.skyfoundry.core.island.IslandMember;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class IslandMembershipIndex {

    private final Map<UUID, IslandMember> members = new HashMap<>();

    public void clear() {
        members.clear();
    }

    public void registerMember(
            IslandMember member) {
        members.put(
                member.getPlayerUuid(),
                member);
    }

    public void unregisterMember(
            UUID playerUuid) {
        members.remove(
                playerUuid);
    }

    public Optional<IslandMember> getMember(
            UUID playerUuid) {
        return Optional.ofNullable(
                members.get(
                        playerUuid));
    }

    public boolean belongsToIsland(
            UUID playerUuid,
            long islandId) {
        IslandMember member = members.get(
                playerUuid);

        if (member == null) {
            return false;
        }

        return member.getIslandId() == islandId;
    }
}