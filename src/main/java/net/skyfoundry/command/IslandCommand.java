package net.skyfoundry.command;

import net.skyfoundry.SkyFoundry;
import net.skyfoundry.island.Island;
import net.skyfoundry.island.IslandManager;
import net.skyfoundry.schematic.LoadedSchematic;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;

public final class IslandCommand implements CommandExecutor {

        private final SkyFoundry plugin;
        private final IslandManager islandManager;

        private final Map<UUID, Long> deletionConfirmations = new HashMap<>();

        private final Map<UUID, Boolean> creatingIslands = new HashMap<>();

        private final Map<UUID, Boolean> deletingIslands = new HashMap<>();

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
                        if (islandManager.hasIsland(player.getUniqueId())) {
                                teleportHome(player);
                        } else {
                                player.sendRichMessage(
                                                "<gray>Usage: <gold>/island create</gold>");
                        }

                        return true;
                }

                switch (args[0].toLowerCase()) {
                        case "create" -> handleCreate(player);
                        case "home" -> teleportHome(player);
                        case "delete" -> handleDelete(player, args);
                        default -> player.sendRichMessage(
                                        "<gray>Usage: <gold>/island create</gold>, "
                                                        + "<gold>/island home</gold>, "
                                                        + "<gold>/island delete</gold>");
                }

                return true;
        }

        private void handleCreate(Player player) {
                UUID uuid = player.getUniqueId();

                if (islandManager.hasIsland(uuid)) {
                        sendMessage(
                                        player,
                                        "messages.island-already-exists",
                                        "<red>You already own an island.</red>");

                        return;
                }

                if (creatingIslands.containsKey(uuid)) {
                        return;
                }

                creatingIslands.put(
                                uuid,
                                true);

                plugin.getSchematicManager()
                                .loadStarterSchematic()
                                .thenAccept(schematic -> Bukkit
                                                .getScheduler()
                                                .runTask(
                                                                plugin,
                                                                () -> createIslandRecord(
                                                                                player,
                                                                                schematic)))
                                .exceptionally(exception -> {
                                        creatingIslands.remove(uuid);

                                        plugin.getLogger().severe(
                                                        "Failed to load starter schematic for "
                                                                        + player.getName()
                                                                        + ": "
                                                                        + getRootMessage(exception));

                                        Bukkit.getScheduler().runTask(
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
                        creatingIslands.remove(uuid);
                        return;
                }

                if (islandManager.hasIsland(uuid)) {
                        creatingIslands.remove(uuid);

                        sendMessage(
                                        player,
                                        "messages.island-already-exists",
                                        "<red>You already own an island.</red>");

                        return;
                }

                final Island island;

                try {
                        island = islandManager.createIsland(
                                        uuid);
                } catch (SQLException exception) {
                        creatingIslands.remove(uuid);

                        plugin.getLogger().severe(
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
                                .thenRun(() -> {
                                        creatingIslands.remove(uuid);

                                        if (!player.isOnline()) {
                                                return;
                                        }

                                        sendMessage(
                                                        player,
                                                        "messages.island-created",
                                                        "<green>Your island has been created.</green>");

                                        teleportHome(player);
                                })
                                .exceptionally(exception -> {
                                        creatingIslands.remove(uuid);

                                        plugin.getLogger().severe(
                                                        "Failed to paste starter schematic for "
                                                                        + player.getName()
                                                                        + ": "
                                                                        + getRootMessage(exception));

                                        if (player.isOnline()) {
                                                sendMessage(
                                                                player,
                                                                "messages.island-create-failed",
                                                                "<red>Something went wrong while creating your island.</red>");
                                        }

                                        return null;
                                });
        }

        private void teleportHome(Player player) {
                Island island = islandManager
                                .getIsland(player.getUniqueId())
                                .orElse(null);

                if (island == null) {
                        sendMessage(
                                        player,
                                        "messages.island-not-found",
                                        "<red>You do not have an island.</red>");

                        return;
                }

                sendMessage(
                                player,
                                "messages.teleporting",
                                "<gray>Teleporting to your island...</gray>");

                Location home = island.getHome();

                player.teleport(
                                home);
        }

        private void handleDelete(
                        Player player,
                        String[] args) {
                UUID uuid = player.getUniqueId();

                if (!islandManager.hasIsland(uuid)) {
                        sendMessage(
                                        player,
                                        "messages.island-not-found",
                                        "<red>You do not have an island.</red>");

                        return;
                }

                if (deletingIslands.containsKey(uuid)) {
                        player.sendRichMessage(
                                        getPrefix()
                                                        + "<yellow>Your island is already being deleted.</yellow>");

                        return;
                }

                boolean confirmationRequired = plugin.getConfig().getBoolean(
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
                                plugin.getConfig().getInt(
                                                "deletion.confirmation-timeout-seconds",
                                                30));

                long expiration = System.currentTimeMillis()
                                + (timeoutSeconds * 1000L);

                deletionConfirmations.put(
                                uuid,
                                expiration);

                String message = plugin.getConfig().getString(
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

                Long expiration = deletionConfirmations.get(uuid);

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

                if (deletingIslands.containsKey(uuid)) {
                        return;
                }

                deletingIslands.put(
                                uuid,
                                true);

                player.sendRichMessage(
                                getPrefix()
                                                + "<gray>Deleting your island...</gray>");

                islandManager.deleteIsland(uuid)
                                .thenAccept(deleted -> {
                                        deletingIslands.remove(
                                                        uuid);

                                        Bukkit.getScheduler().runTask(
                                                        plugin,
                                                        () -> {
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
                                                                                        "messages.island-not-found",
                                                                                        "<red>You do not have an island.</red>");
                                                                }
                                                        });
                                })
                                .exceptionally(exception -> {
                                        deletingIslands.remove(
                                                        uuid);

                                        plugin.getLogger().severe(
                                                        "Failed to delete island for "
                                                                        + player.getName()
                                                                        + ": "
                                                                        + getRootMessage(exception));

                                        Bukkit.getScheduler().runTask(
                                                        plugin,
                                                        () -> {
                                                                if (player.isOnline()) {
                                                                        sendMessage(
                                                                                        player,
                                                                                        "messages.island-delete-failed",
                                                                                        "<red>Something went wrong while deleting your island.</red>");
                                                                }
                                                        });

                                        return null;
                                });
        }

        private void sendMessage(
                        CommandSender sender,
                        String path,
                        String fallback) {
                String message = plugin.getConfig().getString(
                                path,
                                fallback);

                sender.sendRichMessage(
                                getPrefix()
                                                + message);
        }

        private String getPrefix() {
                return plugin.getConfig().getString(
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
}