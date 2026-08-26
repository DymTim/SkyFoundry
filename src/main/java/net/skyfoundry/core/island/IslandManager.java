package net.skyfoundry.core.island;

import net.skyfoundry.core.config.ConfigManager;
import net.skyfoundry.core.invite.IslandInvite;
import net.skyfoundry.core.invite.IslandInviteManager;
import net.skyfoundry.core.service.IslandCreationService;
import net.skyfoundry.core.world.SkyWorldManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class IslandManager {

    private final ConfigManager configManager;
    private final IslandRepository islandRepository;
    private final IslandCreationService islandCreationService;
    private final IslandInviteManager inviteManager;
    private final SkyWorldManager skyWorldManager;

    public IslandManager(
            ConfigManager configManager,
            IslandRepository islandRepository,
            IslandCreationService islandCreationService,
            IslandInviteManager inviteManager,
            SkyWorldManager skyWorldManager) {
        this.configManager = configManager;
        this.islandRepository = islandRepository;
        this.islandCreationService = islandCreationService;
        this.inviteManager = inviteManager;
        this.skyWorldManager = skyWorldManager;
    }

    public Optional<Island> getIsland(
            UUID playerUuid) {
        return islandRepository.findByMember(
                playerUuid);
    }

    public Optional<IslandMember> getMember(
            UUID playerUuid) {
        return islandRepository.findMember(
                playerUuid);
    }

    public List<IslandMember> getMembers(
            Island island) {
        return islandRepository.getMembers(
                island.getId());
    }

    public boolean hasIsland(UUID playerUuid) {
        return getIsland(
                playerUuid).isPresent();
    }

    public Island createIsland(Player player) {
        if (hasIsland(player.getUniqueId())) {
            throw new IllegalStateException(
                    "Player already belongs to an island.");
        }

        return islandCreationService.createIsland(
                player.getUniqueId());
    }

    public IslandInvite invitePlayer(
            Player inviter,
            Player invited) {
        Island island = getIsland(
                inviter.getUniqueId()).orElseThrow(
                        () -> new IllegalStateException(
                                "You do not belong to an island."));

        IslandMember inviterMember = islandRepository.findMember(
                island.getId(),
                inviter.getUniqueId()).orElseThrow();

        if (!inviterMember
                .getRole()
                .canInvite()) {

            throw new IllegalStateException(
                    "Your island role cannot invite players.");
        }

        if (hasIsland(
                invited.getUniqueId())) {
            throw new IllegalStateException(
                    "That player already belongs to an island.");
        }

        if (islandRepository.countMembers(
                island.getId()) >= configManager.getDefaultMemberLimit()) {

            throw new IllegalStateException(
                    "Your island has reached its member limit.");
        }

        return inviteManager.createInvite(
                island,
                inviter.getUniqueId(),
                invited.getUniqueId());
    }

    public Island acceptInvite(Player player) {
        if (hasIsland(player.getUniqueId())) {
            throw new IllegalStateException(
                    "You already belong to an island.");
        }

        IslandInvite invite = inviteManager.getInvite(
                player.getUniqueId()).orElseThrow(
                        () -> new IllegalStateException(
                                "You do not have an active island invitation."));

        Island island = islandRepository.findById(
                invite.getIslandId()).orElseThrow(
                        () -> new IllegalStateException(
                                "The island that invited you no longer exists."));

        if (islandRepository.countMembers(
                island.getId()) >= configManager.getDefaultMemberLimit()) {

            inviteManager.removeInvite(
                    player.getUniqueId());

            throw new IllegalStateException(
                    "That island is now full.");
        }

        islandRepository.addMember(
                island.getId(),
                player.getUniqueId(),
                IslandRole.MEMBER);

        inviteManager.removeInvite(
                player.getUniqueId());

        return island;
    }

    public boolean declineInvite(Player player) {
        if (!inviteManager.hasActiveInvite(
                player.getUniqueId())) {
            return false;
        }

        inviteManager.removeInvite(
                player.getUniqueId());

        return true;
    }

    public void leaveIsland(Player player) {
        Island island = getIsland(
                player.getUniqueId()).orElseThrow(
                        () -> new IllegalStateException(
                                "You do not belong to an island."));

        IslandMember member = getMember(
                player.getUniqueId()).orElseThrow();

        if (member.getRole() == IslandRole.OWNER) {
            throw new IllegalStateException(
                    "The island owner cannot leave. Transfer ownership or delete the island first.");
        }

        islandRepository.removeMember(
                island.getId(),
                player.getUniqueId());
    }

    public void kickMember(
            Player actor,
            Player target) {
        Island island = requireSameIsland(
                actor,
                target);

        IslandMember actorMember = getMember(
                actor.getUniqueId()).orElseThrow();

        IslandMember targetMember = getMember(
                target.getUniqueId()).orElseThrow();

        if (!actorMember
                .getRole()
                .canKick()) {

            throw new IllegalStateException(
                    "Your role cannot kick island members.");
        }

        if (targetMember.getRole() == IslandRole.OWNER) {

            throw new IllegalStateException(
                    "The island owner cannot be kicked.");
        }

        if (actorMember.getRole() == IslandRole.CO_OWNER
                && targetMember.getRole() == IslandRole.CO_OWNER) {

            throw new IllegalStateException(
                    "Co-Owners cannot kick other Co-Owners.");
        }

        if (actor.getUniqueId().equals(
                target.getUniqueId())) {
            throw new IllegalStateException(
                    "Use /island leave instead.");
        }

        islandRepository.removeMember(
                island.getId(),
                target.getUniqueId());
    }

    public void promoteMember(
            Player actor,
            Player target) {
        Island island = requireSameIsland(
                actor,
                target);

        IslandMember actorMember = getMember(
                actor.getUniqueId()).orElseThrow();

        IslandMember targetMember = getMember(
                target.getUniqueId()).orElseThrow();

        if (!actorMember
                .getRole()
                .canPromote()) {

            throw new IllegalStateException(
                    "Only the island owner can promote members.");
        }

        if (targetMember.getRole() == IslandRole.OWNER) {

            throw new IllegalStateException(
                    "That player is already the owner.");
        }

        if (targetMember.getRole() == IslandRole.CO_OWNER) {

            throw new IllegalStateException(
                    "That player is already a Co-Owner.");
        }

        islandRepository.updateRole(
                island.getId(),
                target.getUniqueId(),
                IslandRole.CO_OWNER);
    }

    public void demoteMember(
            Player actor,
            Player target) {
        Island island = requireSameIsland(
                actor,
                target);

        IslandMember actorMember = getMember(
                actor.getUniqueId()).orElseThrow();

        IslandMember targetMember = getMember(
                target.getUniqueId()).orElseThrow();

        if (!actorMember
                .getRole()
                .canDemote()) {

            throw new IllegalStateException(
                    "Only the island owner can demote members.");
        }

        if (targetMember.getRole() != IslandRole.CO_OWNER) {

            throw new IllegalStateException(
                    "Only Co-Owners can be demoted.");
        }

        islandRepository.updateRole(
                island.getId(),
                target.getUniqueId(),
                IslandRole.MEMBER);
    }

    private Island requireSameIsland(
            Player first,
            Player second) {
        Island firstIsland = getIsland(
                first.getUniqueId()).orElseThrow(
                        () -> new IllegalStateException(
                                "You do not belong to an island."));

        Island secondIsland = getIsland(
                second.getUniqueId()).orElseThrow(
                        () -> new IllegalStateException(
                                "That player does not belong to an island."));

        if (firstIsland.getId() != secondIsland.getId()) {

            throw new IllegalStateException(
                    "That player is not a member of your island.");
        }

        return firstIsland;
    }

    public void teleportHome(Player player) {
        Island island = getIsland(
                player.getUniqueId()).orElseThrow(
                        () -> new IllegalStateException(
                                "Player does not belong to an island."));

        Location location = islandCreationService
                .getHomeLocation(island);

        player.teleport(location);
    }

    public int getIslandCount() {
        return islandRepository.countIslands();
    }

    public SkyWorldManager getSkyWorldManager() {
        return skyWorldManager;
    }
}