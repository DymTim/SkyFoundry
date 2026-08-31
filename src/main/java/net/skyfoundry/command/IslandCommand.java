package net.skyfoundry.command;

import net.skyfoundry.SkyFoundry;
import net.skyfoundry.island.Island;
import net.skyfoundry.island.IslandManager;
import net.skyfoundry.island.IslandRole;
import net.skyfoundry.schematic.LoadedSchematic;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

public final class IslandCommand
                implements CommandExecutor {

        private final SkyFoundry plugin;
        private final IslandManager islandManager;

        private final Map<UUID, Long> deletionConfirmations = new ConcurrentHashMap<>();
        private final Map<UUID, PendingTransfer> transferConfirmations = new ConcurrentHashMap<>();

        private final Set<UUID> creatingIslands = ConcurrentHashMap.newKeySet();

        private final Set<UUID> deletingIslands = ConcurrentHashMap.newKeySet();

        public IslandCommand(
                        SkyFoundry plugin,
                        IslandManager islandManager) {
                this.plugin = plugin;
                this.islandManager = islandManager;
        }

        @Override
        public boolean onCommand(
                        @NotNull CommandSender sender,
                        @NotNull Command command,
                        @NotNull String label,
                        @NotNull String[] args) {
                if (!(sender instanceof Player player)) {
                        sendMessage(
                                        sender,
                                        "messages.player-only",
                                        "<red>This command can only be used by a player.</red>");

                        return true;
                }

                if (args.length == 0) {
                        if (islandManager.hasIsland(
                                        player.getUniqueId())) {
                                teleportHome(
                                                player);

                        } else {
                                sendUsage(
                                                player);
                        }

                        return true;
                }

                switch (args[0].toLowerCase()) {
                        case "create" ->
                                handleCreate(
                                                player);

                        case "home" ->
                                teleportHome(
                                                player);

                        case "sethome" ->
                                handleSetHome(
                                                player);

                        case "invite" ->
                                handleInvite(
                                                player,
                                                args);

                        case "accept" ->
                                handleAccept(
                                                player);

                        case "leave" ->
                                handleLeave(
                                                player);

                        case "role" ->
                                handleRole(
                                                player,
                                                args);

                        case "delete" ->
                                handleDelete(
                                                player,
                                                args);

                        case "transfer" ->
                                handleTransfer(
                                                player,
                                                args);

                        case "kick" ->
                                handleKick(
                                                player,
                                                args);

                        default ->
                                sendUsage(
                                                player);
                }

                return true;
        }

        private void handleCreate(
                        Player player) {
                UUID uuid = player.getUniqueId();

                if (islandManager.hasIsland(
                                uuid)) {
                        sendMessage(
                                        player,
                                        "messages.island-already-exists",
                                        "<red>You are already part of an island.</red>");

                        return;
                }

                if (!creatingIslands.add(
                                uuid)) {
                        return;
                }

                plugin.getSchematicManager()
                                .loadStarterSchematic()
                                .thenAccept(
                                                schematic -> Bukkit.getScheduler()
                                                                .runTask(
                                                                                plugin,
                                                                                () -> createIslandRecord(
                                                                                                player,
                                                                                                schematic)))
                                .exceptionally(
                                                exception -> {
                                                        creatingIslands.remove(
                                                                        uuid);

                                                        plugin.getLogger()
                                                                        .severe(
                                                                                        "Failed to load starter schematic for "
                                                                                                        + player.getName()
                                                                                                        + ": "
                                                                                                        + getRootMessage(
                                                                                                                        exception));

                                                        Bukkit.getScheduler()
                                                                        .runTask(
                                                                                        plugin,
                                                                                        () -> sendMessage(
                                                                                                        player,
                                                                                                        "messages.island-create-failed",
                                                                                                        "<red>Something went wrong while creating your island.</red>"));

                                                        return null;
                                                });
        }

        private void createIslandRecord(
                        Player player,
                        LoadedSchematic schematic) {
                UUID uuid = player.getUniqueId();

                if (!player.isOnline()) {
                        creatingIslands.remove(
                                        uuid);

                        return;
                }

                if (islandManager.hasIsland(
                                uuid)) {
                        creatingIslands.remove(
                                        uuid);

                        sendMessage(
                                        player,
                                        "messages.island-already-exists",
                                        "<red>You are already part of an island.</red>");

                        return;
                }

                final Island island;

                try {
                        island = islandManager.createIsland(
                                        uuid);

                } catch (SQLException exception) {
                        creatingIslands.remove(
                                        uuid);

                        plugin.getLogger()
                                        .severe(
                                                        "Failed to create island record for "
                                                                        + player.getName()
                                                                        + ": "
                                                                        + exception.getMessage());

                        sendMessage(
                                        player,
                                        "messages.island-create-failed",
                                        "<red>Something went wrong while creating your island.</red>");

                        return;
                }

                plugin.getSchematicManager()
                                .paste(
                                                island,
                                                schematic)
                                .thenRun(
                                                () -> {
                                                        creatingIslands.remove(
                                                                        uuid);

                                                        if (!player.isOnline()) {
                                                                return;
                                                        }

                                                        sendMessage(
                                                                        player,
                                                                        "messages.island-created",
                                                                        "<green>Your island has been created.</green>");

                                                        teleportHome(
                                                                        player);
                                                })
                                .exceptionally(
                                                exception -> {
                                                        creatingIslands.remove(
                                                                        uuid);

                                                        plugin.getLogger()
                                                                        .severe(
                                                                                        "Failed to paste starter schematic for "
                                                                                                        + player.getName()
                                                                                                        + ": "
                                                                                                        + getRootMessage(
                                                                                                                        exception));

                                                        Bukkit.getScheduler()
                                                                        .runTask(
                                                                                        plugin,
                                                                                        () -> {
                                                                                                islandManager
                                                                                                                .deleteIsland(
                                                                                                                                uuid);

                                                                                                if (player.isOnline()) {
                                                                                                        sendMessage(
                                                                                                                        player,
                                                                                                                        "messages.island-create-failed",
                                                                                                                        "<red>Something went wrong while creating your island.</red>");
                                                                                                }
                                                                                        });

                                                        return null;
                                                });
        }

        private void teleportHome(
                        Player player) {
                Location home = islandManager.getHome(
                                player.getUniqueId());

                if (home == null) {
                        sendMessage(
                                        player,
                                        "messages.island-not-found",
                                        "<red>You are not part of an island.</red>");

                        return;
                }

                sendMessage(
                                player,
                                "messages.teleporting",
                                "<gray>Teleporting to your island...</gray>");

                player.teleport(
                                home);
        }

        private void handleSetHome(
                        Player player) {
                UUID uuid = player.getUniqueId();

                Island island = islandManager
                                .getIsland(
                                                uuid)
                                .orElse(null);

                if (island == null) {
                        sendMessage(
                                        player,
                                        "messages.island-not-found",
                                        "<red>You are not part of an island.</red>");

                        return;
                }

                if (!island.contains(
                                player.getLocation(),
                                Math.max(
                                                1,
                                                plugin.getConfig().getInt(
                                                                "islands.size",
                                                                50)))) {
                        sendMessage(
                                        player,
                                        "messages.sethome-outside-island",
                                        "<red>You must be standing inside your island to set your home.</red>");

                        return;
                }

                try {
                        if (!islandManager.setHome(
                                        uuid,
                                        player.getLocation())) {
                                sendMessage(
                                                player,
                                                "messages.sethome-failed",
                                                "<red>Could not set your island home.</red>");

                                return;
                        }

                        sendMessage(
                                        player,
                                        "messages.sethome-success",
                                        "<green>Your island home has been updated.</green>");

                } catch (SQLException exception) {
                        plugin.getLogger()
                                        .severe(
                                                        "Failed to set island home for "
                                                                        + player.getName()
                                                                        + ": "
                                                                        + exception.getMessage());

                        sendMessage(
                                        player,
                                        "messages.sethome-failed",
                                        "<red>Could not set your island home.</red>");
                }
        }

        private void handleInvite(
                        Player player,
                        String[] args) {
                if (args.length < 2) {
                        player.sendRichMessage(
                                        getPrefix()
                                                        + "<gray>Usage: <gold>/island invite <player></gold></gray>");

                        return;
                }

                IslandRole role = islandManager
                                .getRole(
                                                player.getUniqueId())
                                .orElse(null);

                if (role == null) {
                        sendMessage(
                                        player,
                                        "messages.island-not-found",
                                        "<red>You are not part of an island.</red>");

                        return;
                }

                if (!role.canInvite()) {
                        sendMessage(
                                        player,
                                        "messages.invite-no-permission",
                                        "<red>Your island role cannot invite players.</red>");

                        return;
                }

                Player target = Bukkit.getPlayerExact(
                                args[1]);

                if (target == null) {
                        sendMessage(
                                        player,
                                        "messages.player-not-found",
                                        "<red>That player is not online.</red>");

                        return;
                }

                if (target.getUniqueId()
                                .equals(
                                                player.getUniqueId())) {

                        sendMessage(
                                        player,
                                        "messages.invite-self",
                                        "<red>You cannot invite yourself.</red>");

                        return;
                }

                if (islandManager.hasIsland(
                                target.getUniqueId())) {
                        sendMessage(
                                        player,
                                        "messages.invite-target-has-island",
                                        "<red>That player is already part of an island.</red>");

                        return;
                }

                try {
                        if (!islandManager.invite(
                                        player.getUniqueId(),
                                        target.getUniqueId())) {
                                sendMessage(
                                                player,
                                                "messages.invite-failed",
                                                "<red>That player could not be invited.</red>");

                                return;
                        }

                        sendMessage(
                                        player,
                                        "messages.invite-sent",
                                        "<green>Island invitation sent.</green>");

                        String invited = plugin.getConfig()
                                        .getString(
                                                        "messages.invite-received",
                                                        "<yellow>{player}</yellow> invited you to their island. Run <gold>/island accept</gold> to join.");

                        invited = invited.replace(
                                        "{player}",
                                        player.getName());

                        target.sendRichMessage(
                                        getPrefix()
                                                        + invited);

                } catch (SQLException exception) {
                        plugin.getLogger()
                                        .severe(
                                                        "Failed to create island invite: "
                                                                        + exception.getMessage());

                        sendMessage(
                                        player,
                                        "messages.invite-failed",
                                        "<red>That player could not be invited.</red>");
                }
        }

        private void handleAccept(
                        Player player) {
                if (islandManager.hasIsland(
                                player.getUniqueId())) {
                        sendMessage(
                                        player,
                                        "messages.island-already-exists",
                                        "<red>You are already part of an island.</red>");

                        return;
                }

                try {
                        Optional<Island> island = islandManager.acceptLatestInvite(
                                        player.getUniqueId());

                        if (island.isEmpty()) {
                                sendMessage(
                                                player,
                                                "messages.invite-none",
                                                "<red>You do not have a valid island invitation.</red>");

                                return;
                        }

                        sendMessage(
                                        player,
                                        "messages.invite-accepted",
                                        "<green>You joined the island.</green>");

                        teleportHome(
                                        player);

                } catch (SQLException exception) {
                        plugin.getLogger()
                                        .severe(
                                                        "Failed to accept island invite for "
                                                                        + player.getName()
                                                                        + ": "
                                                                        + exception.getMessage());

                        sendMessage(
                                        player,
                                        "messages.invite-accept-failed",
                                        "<red>Could not join the island.</red>");
                }
        }

        private void handleLeave(
                        Player player) {
                IslandRole role = islandManager
                                .getRole(
                                                player.getUniqueId())
                                .orElse(null);

                if (role == null) {
                        sendMessage(
                                        player,
                                        "messages.island-not-found",
                                        "<red>You are not part of an island.</red>");

                        return;
                }

                if (role == IslandRole.OWNER) {
                        sendMessage(
                                        player,
                                        "messages.owner-cannot-leave",
                                        "<red>Island owners must delete the island instead.</red>");

                        return;
                }

                try {
                        if (!islandManager.leaveIsland(
                                        player.getUniqueId())) {
                                sendMessage(
                                                player,
                                                "messages.leave-failed",
                                                "<red>Could not leave the island.</red>");

                                return;
                        }

                        player.teleport(
                                        islandManager.getSafeTeleportLocation());

                        sendMessage(
                                        player,
                                        "messages.island-left",
                                        "<green>You left the island.</green>");

                } catch (SQLException exception) {
                        plugin.getLogger()
                                        .severe(
                                                        "Failed to remove island member "
                                                                        + player.getName()
                                                                        + ": "
                                                                        + exception.getMessage());

                        sendMessage(
                                        player,
                                        "messages.leave-failed",
                                        "<red>Could not leave the island.</red>");
                }
        }

        private void handleRole(
                        Player player,
                        String[] args) {
                if (args.length < 3) {
                        player.sendRichMessage(
                                        getPrefix()
                                                        + "<gray>Usage: <gold>/island role <player> <member|co_owner></gold></gray>");

                        return;
                }

                IslandRole actorRole = islandManager
                                .getRole(
                                                player.getUniqueId())
                                .orElse(null);

                if (actorRole == null
                                || !actorRole.canChangeRoles()) {

                        sendMessage(
                                        player,
                                        "messages.role-no-permission",
                                        "<red>Only the island owner can change member roles.</red>");

                        return;
                }

                Player target = Bukkit.getPlayerExact(
                                args[1]);

                if (target == null) {
                        sendMessage(
                                        player,
                                        "messages.player-not-found",
                                        "<red>That player is not online.</red>");

                        return;
                }

                IslandRole newRole;

                switch (args[2].toLowerCase()) {
                        case "member" ->
                                newRole = IslandRole.MEMBER;

                        case "co_owner",
                                        "co-owner",
                                        "coowner" ->
                                newRole = IslandRole.CO_OWNER;

                        default -> {
                                player.sendRichMessage(
                                                getPrefix()
                                                                + "<gray>Role must be <gold>member</gold> or <gold>co_owner</gold>.</gray>");

                                return;
                        }
                }

                try {
                        if (!islandManager.setRole(
                                        player.getUniqueId(),
                                        target.getUniqueId(),
                                        newRole)) {
                                sendMessage(
                                                player,
                                                "messages.role-change-failed",
                                                "<red>Could not change that player's island role.</red>");

                                return;
                        }

                        String message = plugin.getConfig()
                                        .getString(
                                                        "messages.role-changed",
                                                        "<green>{player}'s role is now <yellow>{role}</yellow>.</green>");

                        message = message.replace(
                                        "{player}",
                                        target.getName())
                                        .replace(
                                                        "{role}",
                                                        formatRole(
                                                                        newRole));

                        player.sendRichMessage(
                                        getPrefix()
                                                        + message);

                        String targetMessage = plugin.getConfig()
                                        .getString(
                                                        "messages.your-role-changed",
                                                        "<green>Your island role is now <yellow>{role}</yellow>.</green>");

                        targetMessage = targetMessage.replace(
                                        "{role}",
                                        formatRole(
                                                        newRole));

                        target.sendRichMessage(
                                        getPrefix()
                                                        + targetMessage);

                } catch (SQLException exception) {
                        plugin.getLogger()
                                        .severe(
                                                        "Failed to update island role: "
                                                                        + exception.getMessage());

                        sendMessage(
                                        player,
                                        "messages.role-change-failed",
                                        "<red>Could not change that player's island role.</red>");
                }
        }

        private void handleDelete(
                        Player player,
                        String[] args) {
                UUID uuid = player.getUniqueId();

                IslandRole role = islandManager
                                .getRole(
                                                uuid)
                                .orElse(null);

                if (role == null) {
                        sendMessage(
                                        player,
                                        "messages.island-not-found",
                                        "<red>You are not part of an island.</red>");

                        return;
                }

                if (!role.canDeleteIsland()) {
                        sendMessage(
                                        player,
                                        "messages.delete-owner-only",
                                        "<red>Only the island owner can delete the island.</red>");

                        return;
                }

                if (deletingIslands.contains(
                                uuid)) {
                        sendMessage(
                                        player,
                                        "messages.island-already-deleting",
                                        "<yellow>Your island is already being deleted.</yellow>");

                        return;
                }

                boolean confirmationRequired = plugin.getConfig()
                                .getBoolean(
                                                "deletion.require-confirmation",
                                                true);

                if (!confirmationRequired) {
                        performDelete(
                                        player);

                        return;
                }

                if (args.length >= 2
                                && args[1].equalsIgnoreCase(
                                                "confirm")) {
                        confirmDelete(
                                        player);

                        return;
                }

                int timeoutSeconds = Math.max(
                                1,
                                plugin.getConfig()
                                                .getInt(
                                                                "deletion.confirmation-timeout-seconds",
                                                                30));

                deletionConfirmations.put(
                                uuid,
                                System.currentTimeMillis()
                                                + timeoutSeconds * 1000L);

                String message = plugin.getConfig()
                                .getString(
                                                "messages.island-delete-confirm",
                                                "<red>Run <gold>/island delete confirm</gold> within <yellow>{seconds}</yellow> seconds to permanently delete your island.</red>");

                message = message.replace(
                                "{seconds}",
                                Integer.toString(
                                                timeoutSeconds));

                player.sendRichMessage(
                                getPrefix()
                                                + message);
        }

        private void confirmDelete(
                        Player player) {
                UUID uuid = player.getUniqueId();

                Long expiration = deletionConfirmations.get(
                                uuid);

                if (expiration == null
                                || System.currentTimeMillis() > expiration) {

                        deletionConfirmations.remove(
                                        uuid);

                        sendMessage(
                                        player,
                                        "messages.island-delete-confirm-expired",
                                        "<red>Your island deletion confirmation has expired.</red>");

                        return;
                }

                deletionConfirmations.remove(
                                uuid);

                performDelete(
                                player);
        }

        private void performDelete(
                        Player player) {
                UUID uuid = player.getUniqueId();

                if (!deletingIslands.add(
                                uuid)) {
                        return;
                }

                sendMessage(
                                player,
                                "messages.island-deleting",
                                "<gray>Deleting your island...</gray>");

                islandManager.deleteIsland(
                                uuid).thenAccept(
                                                deleted -> {
                                                        deletingIslands.remove(
                                                                        uuid);

                                                        if (!player.isOnline()) {
                                                                return;
                                                        }

                                                        if (deleted) {
                                                                sendMessage(
                                                                                player,
                                                                                "messages.island-deleted",
                                                                                "<green>Your island has been deleted.</green>");

                                                        } else {
                                                                sendMessage(
                                                                                player,
                                                                                "messages.island-delete-failed",
                                                                                "<red>Something went wrong while deleting your island.</red>");
                                                        }
                                                })
                                .exceptionally(
                                                exception -> {
                                                        deletingIslands.remove(
                                                                        uuid);

                                                        plugin.getLogger()
                                                                        .severe(
                                                                                        "Failed to delete island for "
                                                                                                        + player.getName()
                                                                                                        + ": "
                                                                                                        + getRootMessage(
                                                                                                                        exception));

                                                        if (player.isOnline()) {
                                                                sendMessage(
                                                                                player,
                                                                                "messages.island-delete-failed",
                                                                                "<red>Something went wrong while deleting your island.</red>");
                                                        }

                                                        return null;
                                                });
        }

        private String formatRole(
                        IslandRole role) {
                return switch (role) {
                        case OWNER ->
                                "Owner";

                        case CO_OWNER ->
                                "Co-Owner";

                        case MEMBER ->
                                "Member";
                };
        }

        private void sendUsage(
                        Player player) {
                player.sendRichMessage(
                                getPrefix()
                                                + "<gray>Commands: "
                                                + "<gold>/island create</gold>, "
                                                + "<gold>/island home</gold>, "
                                                + "<gold>/island sethome</gold>, "
                                                + "<gold>/island invite</gold>, "
                                                + "<gold>/island accept</gold>, "
                                                + "<gold>/island leave</gold>, "
                                                + "<gold>/island role</gold>, "
                                                + "<gold>/island delete</gold>, "
                                                + "<gold>/island transfer</gold>, "
                                                + "<gold>/island kick</gold>, "
                                                + "</gray>");
        }

        private void sendMessage(
                        CommandSender sender,
                        String path,
                        String fallback) {
                String message = plugin.getConfig()
                                .getString(
                                                path,
                                                fallback);

                sender.sendRichMessage(
                                getPrefix()
                                                + message);
        }

        private String getPrefix() {
                return plugin.getConfig()
                                .getString(
                                                "messages.prefix",
                                                "<gold><bold>⚙ SKYFOUNDRY</bold></gold> <dark_gray>┃</dark_gray> ");
        }

        private String getRootMessage(
                        Throwable throwable) {
                Throwable current = throwable;

                while (current instanceof CompletionException
                                && current.getCause() != null) {

                        current = current.getCause();
                }

                String message = current.getMessage();

                if (message == null
                                || message.isBlank()) {

                        return current
                                        .getClass()
                                        .getSimpleName();
                }

                return message;
        }

        private record PendingTransfer(
                        UUID targetUuid,
                        long expiresAt) {
        }

        private void handleTransfer(
                        Player player,
                        String[] args) {
                UUID ownerUuid = player.getUniqueId();

                IslandRole role = islandManager
                                .getRole(
                                                ownerUuid)
                                .orElse(null);

                if (role == null
                                || !role.canTransferOwnership()) {

                        sendMessage(
                                        player,
                                        "messages.transfer-owner-only",
                                        "<red>Only the island owner can transfer ownership.</red>");

                        return;
                }

                if (args.length >= 3
                                && args[1].equalsIgnoreCase(
                                                "confirm")) {
                        confirmTransfer(
                                        player,
                                        args[2]);

                        return;
                }

                if (args.length < 2) {
                        player.sendRichMessage(
                                        getPrefix()
                                                        + "<gray>Usage: <gold>/island transfer <player></gold></gray>");

                        return;
                }

                Player target = Bukkit.getPlayerExact(
                                args[1]);

                if (target == null) {
                        sendMessage(
                                        player,
                                        "messages.player-not-found",
                                        "<red>That player is not online.</red>");

                        return;
                }

                UUID targetUuid = target.getUniqueId();

                if (ownerUuid.equals(
                                targetUuid)) {
                        sendMessage(
                                        player,
                                        "messages.transfer-self",
                                        "<red>You already own the island.</red>");

                        return;
                }

                Island ownerIsland = islandManager
                                .getIsland(
                                                ownerUuid)
                                .orElse(null);

                Island targetIsland = islandManager
                                .getIsland(
                                                targetUuid)
                                .orElse(null);

                if (ownerIsland == null
                                || targetIsland == null
                                || ownerIsland.getIslandId() != targetIsland.getIslandId()) {

                        sendMessage(
                                        player,
                                        "messages.transfer-not-member",
                                        "<red>That player must already be a member of your island.</red>");

                        return;
                }

                int timeoutSeconds = Math.max(
                                1,
                                plugin.getConfig()
                                                .getInt(
                                                                "transfer.confirmation-timeout-seconds",
                                                                30));

                transferConfirmations.put(
                                ownerUuid,
                                new PendingTransfer(
                                                targetUuid,
                                                System.currentTimeMillis()
                                                                + timeoutSeconds * 1000L));

                String message = plugin.getConfig()
                                .getString(
                                                "messages.transfer-confirm",
                                                "<red>Run <gold>/island transfer confirm {player}</gold> within <yellow>{seconds}</yellow> seconds to transfer ownership.</red>");

                message = message.replace(
                                "{player}",
                                target.getName())
                                .replace(
                                                "{seconds}",
                                                Integer.toString(
                                                                timeoutSeconds));

                player.sendRichMessage(
                                getPrefix()
                                                + message);
        }

        private void confirmTransfer(
                        Player player,
                        String targetName) {
                UUID ownerUuid = player.getUniqueId();

                PendingTransfer pending = transferConfirmations.get(
                                ownerUuid);

                if (pending == null
                                || System.currentTimeMillis() > pending.expiresAt()) {

                        transferConfirmations.remove(
                                        ownerUuid);

                        sendMessage(
                                        player,
                                        "messages.transfer-confirm-expired",
                                        "<red>Your ownership transfer confirmation has expired.</red>");

                        return;
                }

                Player target = Bukkit.getPlayer(
                                pending.targetUuid());

                if (target == null
                                || !target.getName()
                                                .equalsIgnoreCase(
                                                                targetName)) {

                        sendMessage(
                                        player,
                                        "messages.transfer-confirm-mismatch",
                                        "<red>That does not match your pending ownership transfer.</red>");

                        return;
                }

                try {
                        boolean transferred = islandManager
                                        .transferOwnership(
                                                        ownerUuid,
                                                        pending.targetUuid());

                        if (!transferred) {
                                sendMessage(
                                                player,
                                                "messages.transfer-failed",
                                                "<red>Could not transfer island ownership.</red>");

                                return;
                        }

                        transferConfirmations.remove(
                                        ownerUuid);

                        String ownerMessage = plugin.getConfig()
                                        .getString(
                                                        "messages.transfer-success",
                                                        "<green>Island ownership has been transferred to <yellow>{player}</yellow>.</green>");

                        ownerMessage = ownerMessage.replace(
                                        "{player}",
                                        target.getName());

                        player.sendRichMessage(
                                        getPrefix()
                                                        + ownerMessage);

                        String targetMessage = plugin.getConfig()
                                        .getString(
                                                        "messages.transfer-received",
                                                        "<green>You are now the owner of this island.</green>");

                        target.sendRichMessage(
                                        getPrefix()
                                                        + targetMessage);

                } catch (SQLException exception) {
                        plugin.getLogger()
                                        .severe(
                                                        "Failed to transfer island ownership: "
                                                                        + exception.getMessage());

                        sendMessage(
                                        player,
                                        "messages.transfer-failed",
                                        "<red>Could not transfer island ownership.</red>");
                }
        }

        private void handleKick(
                        Player player,
                        String[] args) {
                if (args.length < 2) {
                        player.sendRichMessage(
                                        getPrefix()
                                                        + "<gray>Usage: <gold>/island kick <player></gold></gray>");

                        return;
                }

                IslandRole actorRole = islandManager
                                .getRole(
                                                player.getUniqueId())
                                .orElse(null);

                if (actorRole == null
                                || !actorRole.canKick()) {

                        sendMessage(
                                        player,
                                        "messages.kick-no-permission",
                                        "<red>Your island role cannot kick members.</red>");

                        return;
                }

                Player target = Bukkit.getPlayerExact(
                                args[1]);

                if (target == null) {
                        sendMessage(
                                        player,
                                        "messages.player-not-found",
                                        "<red>That player must be online to be kicked.</red>");

                        return;
                }

                if (target.getUniqueId()
                                .equals(
                                                player.getUniqueId())) {

                        sendMessage(
                                        player,
                                        "messages.kick-self",
                                        "<red>You cannot kick yourself.</red>");

                        return;
                }

                IslandRole targetRole = islandManager
                                .getRole(
                                                target.getUniqueId())
                                .orElse(null);

                if (targetRole == null) {
                        sendMessage(
                                        player,
                                        "messages.kick-not-member",
                                        "<red>That player is not a member of your island.</red>");

                        return;
                }

                if (targetRole == IslandRole.OWNER) {
                        sendMessage(
                                        player,
                                        "messages.kick-owner",
                                        "<red>The island owner cannot be kicked.</red>");

                        return;
                }

                if (actorRole == IslandRole.CO_OWNER
                                && targetRole == IslandRole.CO_OWNER) {

                        sendMessage(
                                        player,
                                        "messages.kick-role-too-high",
                                        "<red>Co-Owners cannot kick other Co-Owners.</red>");

                        return;
                }

                try {
                        if (!islandManager.kickMember(
                                        player.getUniqueId(),
                                        target.getUniqueId())) {
                                sendMessage(
                                                player,
                                                "messages.kick-failed",
                                                "<red>Could not remove that island member.</red>");

                                return;
                        }

                        String success = plugin.getConfig()
                                        .getString(
                                                        "messages.kick-success",
                                                        "<green>{player} has been removed from the island.</green>");

                        success = success.replace(
                                        "{player}",
                                        target.getName());

                        player.sendRichMessage(
                                        getPrefix()
                                                        + success);

                        sendMessage(
                                        target,
                                        "messages.kicked",
                                        "<red>You have been removed from the island.</red>");

                } catch (SQLException exception) {
                        plugin.getLogger()
                                        .severe(
                                                        "Failed to kick island member: "
                                                                        + exception.getMessage());

                        sendMessage(
                                        player,
                                        "messages.kick-failed",
                                        "<red>Could not remove that island member.</red>");
                }
        }
}