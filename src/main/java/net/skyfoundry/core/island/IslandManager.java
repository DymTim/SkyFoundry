package net.skyfoundry.core.island;

import net.skyfoundry.core.config.ConfigManager;
import net.skyfoundry.core.confirmation.IslandActionType;
import net.skyfoundry.core.confirmation.IslandConfirmationManager;
import net.skyfoundry.core.confirmation.PendingIslandAction;
import net.skyfoundry.core.home.IslandHome;
import net.skyfoundry.core.home.IslandHomeRepository;
import net.skyfoundry.core.invite.IslandInvite;
import net.skyfoundry.core.invite.IslandInviteManager;
import net.skyfoundry.core.reset.PlayerResetRepository;
import net.skyfoundry.core.service.IslandCreationService;
import net.skyfoundry.core.service.IslandDeletionService;
import net.skyfoundry.core.service.IslandRegionService;
import net.skyfoundry.core.service.IslandResetService;
import net.skyfoundry.core.world.SkyWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class IslandManager {

    private final ConfigManager configManager;

    private final IslandRepository islandRepository;
    private final IslandHomeRepository homeRepository;
    private final PlayerResetRepository resetRepository;

    private final IslandCreationService creationService;
    private final IslandInviteManager inviteManager;
    private final IslandConfirmationManager confirmationManager;
    private final IslandRegionService regionService;
    private final IslandDeletionService deletionService;
    private final IslandResetService resetService;

    private final SkyWorldManager skyWorldManager;

    public IslandManager(
            ConfigManager configManager,
            IslandRepository islandRepository,
            IslandHomeRepository homeRepository,
            PlayerResetRepository resetRepository,
            IslandCreationService creationService,
            IslandInviteManager inviteManager,
            IslandConfirmationManager confirmationManager,
            IslandRegionService regionService,
            IslandDeletionService deletionService,
            IslandResetService resetService,
            SkyWorldManager skyWorldManager) {
        this.configManager = configManager;
        this.islandRepository = islandRepository;
        this.homeRepository = homeRepository;
        this.resetRepository = resetRepository;
        this.creationService = creationService;
        this.inviteManager = inviteManager;
        this.confirmationManager = confirmationManager;
        this.regionService = regionService;
        this.deletionService = deletionService;
        this.resetService = resetService;
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
        if (hasIsland(
                player.getUniqueId())) {

            throw new IllegalStateException(
                    "You already belong to an island.");
        }

        return creationService.createIsland(
                player.getUniqueId());
    }

    public void setHome(Player player) {
        Island island = requireIsland(
                player);

        Location location = player.getLocation();

        if (location.getWorld() == null
                || !location
                        .getWorld()
                        .getName()
                        .equals(
                                island.getWorldName())) {

            throw new IllegalStateException(
                    "You must be on your island to set your home.");
        }

        if (!island.contains(
                location.getBlockX(),
                location.getBlockZ())) {

            throw new IllegalStateException(
                    "Your island home must be inside your island boundary.");
        }

        homeRepository.saveHome(
                island.getId(),
                player.getUniqueId(),
                location);
    }

    public void teleportHome(Player player) {
        Island island = requireIsland(
                player);

        Optional<IslandHome> customHome = homeRepository.findHome(
                island.getId(),
                player.getUniqueId());

        if (customHome.isEmpty()) {

            player.teleport(
                    creationService
                            .getDefaultHomeLocation(
                                    island));

            return;
        }

        IslandHome home = customHome.get();

        World world = Bukkit.getWorld(
                home.getWorldName());

        if (world == null) {

            player.teleport(
                    creationService
                            .getDefaultHomeLocation(
                                    island));

            return;
        }

        player.teleport(
                new Location(
                        world,
                        home.getX(),
                        home.getY(),
                        home.getZ(),
                        home.getYaw(),
                        home.getPitch()));
    }

    public IslandInvite invitePlayer(
            Player inviter,
            Player invited) {
        Island island = requireIsland(
                inviter);

        IslandMember inviterMember = requireMember(
                inviter);

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

    public Island acceptInvite(
            Player player) {
        if (hasIsland(
                player.getUniqueId())) {

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

    public boolean declineInvite(
            Player player) {
        if (!inviteManager.hasActiveInvite(
                player.getUniqueId())) {

            return false;
        }

        inviteManager.removeInvite(
                player.getUniqueId());

        return true;
    }

    public void leaveIsland(
            Player player) {
        Island island = requireIsland(
                player);

        IslandMember member = requireMember(
                player);

        if (member.getRole() == IslandRole.OWNER) {

            throw new IllegalStateException(
                    "The island owner cannot leave. Transfer ownership or delete the island first.");
        }

        homeRepository.deleteHome(
                island.getId(),
                player.getUniqueId());

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

        IslandMember actorMember = requireMember(
                actor);

        IslandMember targetMember = requireMember(
                target);

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

        homeRepository.deleteHome(
                island.getId(),
                target.getUniqueId());

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

        IslandMember actorMember = requireMember(
                actor);

        IslandMember targetMember = requireMember(
                target);

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

        IslandMember actorMember = requireMember(
                actor);

        IslandMember targetMember = requireMember(
                target);

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

    public void requestOwnershipTransfer(
            Player owner,
            Player target) {
        Island island = requireSameIsland(
                owner,
                target);

        IslandMember member = requireMember(
                owner);

        if (!member
                .getRole()
                .canTransferOwnership()) {

            throw new IllegalStateException(
                    "Only the island owner can transfer ownership.");
        }

        if (owner.getUniqueId().equals(
                target.getUniqueId())) {

            throw new IllegalStateException(
                    "You are already the island owner.");
        }

        confirmationManager.create(
                IslandActionType.TRANSFER_OWNERSHIP,
                island.getId(),
                owner.getUniqueId(),
                target.getUniqueId());
    }

    public void requestDeletion(
            Player owner) {
        Island island = requireIsland(
                owner);

        requireOwner(
                owner);

        if (regionService.isBusy(
                island.getId())) {

            throw new IllegalStateException(
                    "Your island is already being modified.");
        }

        confirmationManager.create(
                IslandActionType.DELETE_ISLAND,
                island.getId(),
                owner.getUniqueId(),
                null);
    }

    public void requestReset(
            Player owner) {
        Island island = requireIsland(
                owner);

        requireOwner(
                owner);

        if (regionService.isBusy(
                island.getId())) {

            throw new IllegalStateException(
                    "Your island is already being modified.");
        }

        if (getRemainingResets(
                owner.getUniqueId()) <= 0) {

            throw new IllegalStateException(
                    "You have no lifetime island resets remaining.");
        }

        confirmationManager.create(
                IslandActionType.RESET_ISLAND,
                island.getId(),
                owner.getUniqueId(),
                null);
    }

    public String confirm(
            Player player) {
        PendingIslandAction action = confirmationManager.get(
                player.getUniqueId()).orElseThrow(
                        () -> new IllegalStateException(
                                "You do not have an active island confirmation."));

        confirmationManager.clear(
                player.getUniqueId());

        Island island = islandRepository.findById(
                action.getIslandId()).orElseThrow(
                        () -> new IllegalStateException(
                                "That island no longer exists."));

        if (!island.getOwnerUuid().equals(
                player.getUniqueId())) {

            throw new IllegalStateException(
                    "You are no longer the owner of that island.");
        }

        return switch (action.getType()) {

            case TRANSFER_OWNERSHIP -> {
                UUID targetUuid = action.getTargetUuid();

                if (targetUuid == null) {
                    throw new IllegalStateException(
                            "Ownership transfer target is missing.");
                }

                islandRepository.findMember(
                        island.getId(),
                        targetUuid).orElseThrow(
                                () -> new IllegalStateException(
                                        "That player is no longer a member of your island."));

                islandRepository.transferOwnership(
                        island.getId(),
                        player.getUniqueId(),
                        targetUuid);

                Player target = Bukkit.getPlayer(
                        targetUuid);

                if (target != null) {
                    target.sendMessage(
                            "§aYou are now the owner of the island.");
                }

                yield "§aIsland ownership transferred successfully.";
            }

            case DELETE_ISLAND -> {
                deletionService.deleteIsland(
                        island);

                yield "§eIsland deletion started.";
            }

            case RESET_ISLAND -> {
                if (getRemainingResets(
                        player.getUniqueId()) <= 0) {

                    throw new IllegalStateException(
                            "You no longer have any lifetime resets remaining.");
                }

                resetService.resetIsland(
                        island);

                yield "§eIsland reset started.";
            }
        };
    }

    public boolean cancelConfirmation(
            Player player) {
        return confirmationManager.cancel(
                player.getUniqueId());
    }

    public int getUsedResets(
            UUID playerUuid) {
        return resetRepository.getUsedResets(
                playerUuid);
    }

    public int getRemainingResets(
            UUID playerUuid) {
        return Math.max(
                0,
                configManager.getDefaultLifetimeResets()
                        - getUsedResets(
                                playerUuid));
    }

    public int getIslandCount() {
        return islandRepository.countIslands();
    }

    public SkyWorldManager getSkyWorldManager() {
        return skyWorldManager;
    }

    private Island requireIsland(
            Player player) {
        return getIsland(
                player.getUniqueId()).orElseThrow(
                        () -> new IllegalStateException(
                                "You do not belong to an island."));
    }

    private IslandMember requireMember(
            Player player) {
        return getMember(
                player.getUniqueId()).orElseThrow(
                        () -> new IllegalStateException(
                                "You do not belong to an island."));
    }

    private void requireOwner(
            Player player) {
        IslandMember member = requireMember(
                player);

        if (member.getRole() != IslandRole.OWNER) {

            throw new IllegalStateException(
                    "Only the island owner can do that.");
        }
    }

    private Island requireSameIsland(
            Player first,
            Player second) {
        Island firstIsland = requireIsland(
                first);

        Island secondIsland = requireIsland(
                second);

        if (firstIsland.getId() != secondIsland.getId()) {

            throw new IllegalStateException(
                    "That player is not a member of your island.");
        }

        return firstIsland;
    }
}