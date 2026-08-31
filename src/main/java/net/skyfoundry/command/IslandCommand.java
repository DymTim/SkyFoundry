package net.skyfoundry.command;

import net.skyfoundry.SkyFoundry;
import net.skyfoundry.island.Island;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class IslandCommand implements CommandExecutor {

    private final SkyFoundry plugin;

    private final Map<UUID, Long> deletionConfirmations = new HashMap<>();

    private final Map<UUID, Boolean> creatingIslands = new HashMap<>();

    public IslandCommand(SkyFoundry plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, "messages.player-only");
            return true;
        }

        if (args.length == 0) {
            handleDefault(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> handleCreate(player);
            case "home" -> handleHome(player);
            case "delete" -> handleDelete(player, args);
            default -> sendUsage(player);
        }

        return true;
    }

    private void handleDefault(Player player) {
        if (plugin.getIslandManager().hasIsland(
                player.getUniqueId())) {
            handleHome(player);
            return;
        }

        sendUsage(player);
    }

    private void handleCreate(Player player) {
        UUID uuid = player.getUniqueId();

        if (plugin.getIslandManager().hasIsland(uuid)) {
            send(player, "messages.island-already-exists");
            return;
        }

        if (creatingIslands.containsKey(uuid)) {
            sendRaw(
                    player,
                    "<yellow>Your island is already being created.</yellow>");
            return;
        }

        creatingIslands.put(uuid, true);

        sendRaw(
                player,
                "<gray>Creating your island...</gray>");

        plugin.getSchematicManager()
                .loadStarterSchematic()
                .thenAccept(schematic -> plugin.getServer()
                        .getScheduler()
                        .runTask(
                                plugin,
                                () -> createIslandRecord(
                                        player,
                                        schematic)))
                .exceptionally(exception -> {
                    creatingIslands.remove(uuid);

                    plugin.getLogger().severe(
                            "Failed to load starter schematic: "
                                    + exception.getMessage());

                    plugin.getServer()
                            .getScheduler()
                            .runTask(
                                    plugin,
                                    () -> send(
                                            player,
                                            "messages.island-create-failed"));

                    return null;
                });
    }

    private void createIslandRecord(
            Player player,
            net.skyfoundry.schematic.LoadedSchematic schematic) {
        UUID uuid = player.getUniqueId();

        try {
            Island island = plugin
                    .getIslandManager()
                    .createIsland(uuid);

            plugin.getSchematicManager()
                    .paste(island, schematic)
                    .thenRun(() -> {
                        creatingIslands.remove(uuid);

                        send(
                                player,
                                "messages.island-created");

                        player.teleport(
                                island.getHome());
                    })
                    .exceptionally(exception -> {
                        creatingIslands.remove(uuid);

                        plugin.getLogger().severe(
                                "Failed to paste island for "
                                        + player.getName()
                                        + ": "
                                        + exception.getMessage());

                        send(
                                player,
                                "messages.island-create-failed");

                        return null;
                    });

        } catch (SQLException exception) {
            creatingIslands.remove(uuid);

            plugin.getLogger().severe(
                    "Failed to create island record for "
                            + player.getName()
                            + ": "
                            + exception.getMessage());

            send(
                    player,
                    "messages.island-create-failed");
        }
    }

    private void handleHome(Player player) {
        Optional<Island> islandOptional = plugin
                .getIslandManager()
                .getIsland(player.getUniqueId());

        if (islandOptional.isEmpty()) {
            send(player, "messages.island-not-found");
            return;
        }

        send(player, "messages.teleporting");

        player.teleport(
                islandOptional.get().getHome());
    }

    private void handleDelete(
            Player player,
            String[] args) {
        UUID uuid = player.getUniqueId();

        if (!plugin.getIslandManager().hasIsland(uuid)) {
            send(player, "messages.island-not-found");
            return;
        }

        boolean requireConfirmation = plugin
                .getConfig()
                .getBoolean(
                        "deletion.require-confirmation",
                        true);

        if (!requireConfirmation) {
            deleteIsland(player);
            return;
        }

        if (args.length >= 2
                && args[1].equalsIgnoreCase("confirm")) {
            confirmDelete(player);
            return;
        }

        requestDeleteConfirmation(player);
    }

    private void requestDeleteConfirmation(Player player) {
        int timeout = Math.max(
                1,
                plugin.getConfig().getInt(
                        "deletion.confirmation-timeout-seconds",
                        30));

        deletionConfirmations.put(
                player.getUniqueId(),
                System.currentTimeMillis()
                        + timeout * 1000L);

        String message = plugin.getConfig().getString(
                "messages.island-delete-confirm",
                "<red>Run <gold>/island delete confirm</gold> "
                        + "within <yellow>{seconds}</yellow> seconds.</red>");

        if (message != null && !message.isBlank()) {
            sendRaw(
                    player,
                    message.replace(
                            "{seconds}",
                            Integer.toString(timeout)));
        }
    }

    private void confirmDelete(Player player) {
        UUID uuid = player.getUniqueId();

        Long expiration = deletionConfirmations.remove(uuid);

        if (expiration == null
                || System.currentTimeMillis() > expiration) {
            send(
                    player,
                    "messages.island-delete-confirm-expired");
            return;
        }

        deleteIsland(player);
    }

    private void deleteIsland(Player player) {
        try {
            boolean deleted = plugin
                    .getIslandManager()
                    .deleteIsland(
                            player.getUniqueId());

            deletionConfirmations.remove(
                    player.getUniqueId());

            if (!deleted) {
                send(player, "messages.island-not-found");
                return;
            }

            send(player, "messages.island-deleted");

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to delete island for "
                            + player.getName()
                            + ": "
                            + exception.getMessage());

            send(
                    player,
                    "messages.island-delete-failed");
        }
    }

    private void sendUsage(Player player) {
        sendRaw(
                player,
                "<gold>/island create</gold> "
                        + "<gray>- Create your island.</gray>");

        sendRaw(
                player,
                "<gold>/island home</gold> "
                        + "<gray>- Teleport to your island.</gray>");

        sendRaw(
                player,
                "<gold>/island delete</gold> "
                        + "<gray>- Delete your island.</gray>");
    }

    private void send(
            CommandSender sender,
            String path) {
        String message = plugin
                .getConfig()
                .getString(path);

        if (message == null || message.isBlank()) {
            return;
        }

        sendRaw(sender, message);
    }

    private void sendRaw(
            CommandSender sender,
            String message) {
        String prefix = plugin
                .getConfig()
                .getString(
                        "messages.prefix",
                        "");

        sender.sendRichMessage(
                prefix + message);
    }
}