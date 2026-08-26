package net.skyfoundry.core.command;

import net.skyfoundry.core.island.Island;
import net.skyfoundry.core.island.IslandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

public final class IslandCommand implements CommandExecutor {

    private final IslandManager islandManager;

    public IslandCommand(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(
                    "This command can only be used by players.");

            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);

        switch (subcommand) {
            case "create" -> createIsland(player);
            case "home" -> home(player);
            case "info" -> info(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void createIsland(Player player) {
        if (islandManager.hasIsland(
                player.getUniqueId())) {

            player.sendMessage(
                    "§cYou already belong to an island.");

            return;
        }

        player.sendMessage(
                "§7Creating your SkyFoundry island...");

        try {
            Island island = islandManager.createIsland(player);

            islandManager.teleportHome(player);

            player.sendMessage(
                    "§aYour island has been created.");

            player.sendMessage(
                    "§7Island center: §f"
                            + island.getCenterX()
                            + ", "
                            + island.getCenterZ());

        } catch (Exception exception) {
            player.sendMessage(
                    "§cSomething went wrong while creating your island.");

            exception.printStackTrace();
        }
    }

    private void home(Player player) {
        if (!islandManager.hasIsland(
                player.getUniqueId())) {

            player.sendMessage(
                    "§cYou do not belong to an island.");

            return;
        }

        islandManager.teleportHome(player);

        player.sendMessage(
                "§aTeleported to your island.");
    }

    private void info(Player player) {
        Optional<Island> optionalIsland = islandManager.getIsland(
                player.getUniqueId());

        if (optionalIsland.isEmpty()) {
            player.sendMessage(
                    "§cYou do not belong to an island.");

            return;
        }

        Island island = optionalIsland.get();

        player.sendMessage(
                "§6§lSKYFOUNDRY ISLAND");

        player.sendMessage(
                "§7Owner: §f"
                        + island.getOwnerUuid());

        player.sendMessage(
                "§7Size: §f"
                        + island.getSize()
                        + "x"
                        + island.getSize());

        player.sendMessage(
                "§7Center: §f"
                        + island.getCenterX()
                        + ", "
                        + island.getCenterZ());

        player.sendMessage(
                "§7Slot: §f"
                        + island.getSlotIndex());
    }

    private void sendHelp(Player player) {
        player.sendMessage(
                "§6§lSKYFOUNDRY");

        player.sendMessage(
                "§e/island create §7- Create your island");

        player.sendMessage(
                "§e/island home §7- Return home");

        player.sendMessage(
                "§e/island info §7- View island information");
    }
}