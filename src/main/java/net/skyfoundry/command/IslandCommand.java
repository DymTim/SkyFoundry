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
import java.util.Locale;
import java.util.Optional;

public final class IslandCommand implements CommandExecutor {

    private final SkyFoundry plugin;

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
            default -> sendUsage(player);
        }

        return true;
    }

    private void handleDefault(Player player) {
        if (plugin.getIslandManager().hasIsland(player.getUniqueId())) {
            handleHome(player);
            return;
        }

        sendUsage(player);
    }

    private void handleCreate(Player player) {
        if (plugin.getIslandManager().hasIsland(player.getUniqueId())) {
            send(player, "messages.island-already-exists");
            return;
        }

        try {
            Island island = plugin.getIslandManager().createIsland(
                    player.getUniqueId());

            createTemporaryPlatform(island);

            send(player, "messages.island-created");

            player.teleport(island.getHome());

        } catch (SQLException exception) {
            plugin.getLogger().severe(
                    "Failed to create island for "
                            + player.getName()
                            + ": "
                            + exception.getMessage());

            sendRaw(
                    player,
                    "<red>Something went wrong while creating your island.</red>");
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

        Island island = islandOptional.get();

        send(player, "messages.teleporting");

        player.teleport(island.getHome());
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
        String configuredMaterial = plugin.getConfig().getString(
                "islands.temporary-platform-block",
                "STONE");

        Material material = Material.matchMaterial(
                configuredMaterial);

        if (material == null || !material.isBlock()) {
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
    }

    private void send(CommandSender sender, String path) {
        String message = plugin.getConfig().getString(path);

        if (message == null || message.isBlank()) {
            return;
        }

        sendRaw(sender, message);
    }

    private void sendRaw(CommandSender sender, String message) {
        String prefix = plugin.getConfig().getString(
                "messages.prefix",
                "");

        sender.sendRichMessage(prefix + message);
    }
}