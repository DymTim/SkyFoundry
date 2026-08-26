package net.skyfoundry.core.command;

import net.skyfoundry.core.SkyFoundry;
import net.skyfoundry.core.island.IslandManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class SkyFoundryCommand implements CommandExecutor {

    private final SkyFoundry plugin;
    private final IslandManager islandManager;

    public SkyFoundryCommand(
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
        if (!sender.hasPermission(
                "skyfoundry.admin")) {

            sender.sendMessage(
                    "§cYou do not have permission to use this command.");

            return true;
        }

        if (args.length == 0) {
            sendStatus(sender);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);

        switch (subcommand) {
            case "status" ->
                sendStatus(sender);

            case "reload" ->
                reload(sender);

            default ->
                sendHelp(sender);
        }

        return true;
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(
                "§6§lSKYFOUNDRY STATUS");

        sender.sendMessage(
                "§7Version: §f"
                        + plugin.getPluginMeta().getVersion());

        sender.sendMessage(
                "§7Island world: §f"
                        + islandManager
                                .getSkyWorldManager()
                                .getWorld()
                                .getName());

        sender.sendMessage(
                "§7Loaded islands: §f"
                        + islandManager.getIslandCount());
    }

    private void reload(CommandSender sender) {
        plugin.getConfigManager().reload();

        sender.sendMessage(
                "§aSkyFoundry configuration reloaded.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(
                "§6§lSKYFOUNDRY ADMIN");

        sender.sendMessage(
                "§e/sf status §7- Plugin status");

        sender.sendMessage(
                "§e/sf reload §7- Reload configuration");
    }
}