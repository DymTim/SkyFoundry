package net.skyfoundry.command;

import net.skyfoundry.SkyFoundry;
import net.skyfoundry.island.Island;
import org.bukkit.Material;
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
        UUID playerUuid = player.getUniqueId();

        if (plugin.getIslandManager().hasIsland(playerUuid)) {
            send(
                    player,
                    "messages.island-already-exists");
            return;
        }

        try {
            Island island = plugin
                    .getIslandManager()
                    .createIsland(playerUuid);

            createTemporaryPlatform(island);

            send(
                    player,
                    "messages.island-created");

            player.teleport(island.getHome());

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to create island for "
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
            send(
                    player,
                    "messages.island-not-found");
            return;
        }

        send(
                player,
                "messages.teleporting");

        player.teleport(
                islandOptional.get().getHome());
    }

    private void handleDelete(
            Player player,
            String[] args) {
        UUID playerUuid = player.getUniqueId();

        Optional<Island> islandOptional = plugin
                .getIslandManager()
                .getIsland(playerUuid);

        if (islandOptional.isEmpty()) {
            send(
                    player,
                    "messages.island-not-found");
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
        int timeoutSeconds = Math.max(
                1,
                plugin.getConfig().getInt(
                        "deletion.confirmation-timeout-seconds",
                        30));

        long expiresAt = System.currentTimeMillis()
                + (timeoutSeconds * 1000L);

        deletionConfirmations.put(
                player.getUniqueId(),
                expiresAt);

        String message = plugin.getConfig().getString(
                "messages.island-delete-confirm",
                "<red>Run <gold>/island delete confirm</gold> "
                        + "within <yellow>{seconds}</yellow> seconds "
                        + "to delete your island.</red>");

        if (message == null || message.isBlank()) {
            return;
        }

        message = message.replace(
                "{seconds}",
                Integer.toString(timeoutSeconds));

        sendRaw(player, message);
    }

    private void confirmDelete(Player player) {
        UUID playerUuid = player.getUniqueId();

        Long expiresAt = deletionConfirmations.remove(
                playerUuid);

        if (expiresAt == null
                || System.currentTimeMillis() > expiresAt) {
            send(
                    player,
                    "messages.island-delete-confirm-expired");
            return;
        }

        deleteIsland(player);
    }

    private void deleteIsland(Player player) {
        UUID playerUuid = player.getUniqueId();

        try {
            boolean deleted = plugin
                    .getIslandManager()
                    .deleteIsland(playerUuid);

            deletionConfirmations.remove(playerUuid);

            if (!deleted) {
                send(
                        player,
                        "messages.island-not-found");
                return;
            }

            send(
                    player,
                    "messages.island-deleted");

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

    private void createTemporaryPlatform(Island island) {
        Material material = getPlatformMaterial();

        int y = plugin.getConfig().getInt(
                "islands.creation-y",
                100);

        plugin.getIslandWorld()
                .getBlockAt(
                        island.getCenterX(),
                        y,
                        island.getCenterZ())
                .setType(material, false);
    }

    private Material getPlatformMaterial() {
        String configuredMaterial = plugin
                .getConfig()
                .getString(
                        "islands.temporary-platform-block",
                        "STONE");

        Material material = Material.matchMaterial(
                configuredMaterial);

        if (material == null
                || !material.isBlock()) {
            plugin.getLogger().warning(
                    "Invalid islands.temporary-platform-block '"
                            + configuredMaterial
                            + "'. Using STONE.");

            return Material.STONE;
        }

        return material;
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