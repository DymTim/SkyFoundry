package net.skyfoundry.core.command;

import net.skyfoundry.core.invite.IslandInvite;
import net.skyfoundry.core.island.Island;
import net.skyfoundry.core.island.IslandManager;
import net.skyfoundry.core.island.IslandMember;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class IslandCommand
        implements CommandExecutor {

    private final IslandManager islandManager;

    public IslandCommand(
            IslandManager islandManager) {
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

        String subcommand = args[0].toLowerCase(
                Locale.ROOT);

        switch (subcommand) {

            case "create" ->
                createIsland(player);

            case "home" ->
                home(player);

            case "info" ->
                info(player);

            case "members" ->
                members(player);

            case "invite" ->
                invite(player, args);

            case "accept" ->
                accept(player);

            case "decline" ->
                decline(player);

            case "leave" ->
                leave(player);

            case "kick" ->
                kick(player, args);

            case "promote" ->
                promote(player, args);

            case "demote" ->
                demote(player, args);

            default ->
                sendHelp(player);
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
            Island island = islandManager.createIsland(
                    player);

            islandManager.teleportHome(
                    player);

            player.sendMessage(
                    "§aYour island has been created.");

            player.sendMessage(
                    "§7Island center: §f"
                            + island.getCenterX()
                            + ", "
                            + island.getCenterZ());

        } catch (Exception exception) {

            sendException(
                    player,
                    exception);
        }
    }

    private void home(Player player) {
        try {
            islandManager.teleportHome(
                    player);

            player.sendMessage(
                    "§aTeleported to your island.");

        } catch (Exception exception) {

            sendException(
                    player,
                    exception);
        }
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

        IslandMember member = islandManager.getMember(
                player.getUniqueId()).orElseThrow();

        player.sendMessage(
                "§6§lSKYFOUNDRY ISLAND");

        player.sendMessage(
                "§7Role: §f"
                        + member
                                .getRole()
                                .getDisplayName());

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
                "§7Members: §f"
                        + islandManager
                                .getMembers(island)
                                .size());
    }

    private void members(Player player) {
        Optional<Island> optionalIsland = islandManager.getIsland(
                player.getUniqueId());

        if (optionalIsland.isEmpty()) {

            player.sendMessage(
                    "§cYou do not belong to an island.");

            return;
        }

        List<IslandMember> members = islandManager.getMembers(
                optionalIsland.get());

        player.sendMessage(
                "§6§lISLAND MEMBERS");

        for (IslandMember member : members) {

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(
                    member.getPlayerUuid());

            String name = offlinePlayer.getName();

            if (name == null) {
                name = member.getPlayerUuid()
                        .toString();
            }

            player.sendMessage(
                    "§e"
                            + name
                            + " §8- §7"
                            + member
                                    .getRole()
                                    .getDisplayName());
        }
    }

    private void invite(
            Player player,
            String[] args) {
        if (args.length < 2) {

            player.sendMessage(
                    "§cUsage: /island invite <player>");

            return;
        }

        Player target = Bukkit.getPlayerExact(
                args[1]);

        if (target == null) {

            player.sendMessage(
                    "§cThat player must be online.");

            return;
        }

        if (target.getUniqueId().equals(
                player.getUniqueId())) {

            player.sendMessage(
                    "§cYou cannot invite yourself.");

            return;
        }

        try {
            IslandInvite invite = islandManager.invitePlayer(
                    player,
                    target);

            player.sendMessage(
                    "§aInvited §f"
                            + target.getName()
                            + " §ato your island.");

            target.sendMessage(
                    "§6§lISLAND INVITE");

            target.sendMessage(
                    "§f"
                            + player.getName()
                            + " §7invited you to their island.");

            target.sendMessage(
                    "§e/island accept §7or §e/island decline");

            long seconds = Math.max(
                    0,
                    (invite.getExpiresAt()
                            - System.currentTimeMillis())
                            / 1000);

            target.sendMessage(
                    "§7Expires in approximately §f"
                            + seconds
                            + " seconds§7.");

        } catch (Exception exception) {

            sendException(
                    player,
                    exception);
        }
    }

    private void accept(Player player) {
        try {
            Island island = islandManager.acceptInvite(
                    player);

            player.sendMessage(
                    "§aYou joined the island.");

            islandManager.teleportHome(
                    player);

            notifyIsland(
                    island,
                    "§e"
                            + player.getName()
                            + " §7joined the island.",
                    player);

        } catch (Exception exception) {

            sendException(
                    player,
                    exception);
        }
    }

    private void decline(Player player) {
        if (!islandManager.declineInvite(
                player)) {

            player.sendMessage(
                    "§cYou do not have an active island invitation.");

            return;
        }

        player.sendMessage(
                "§7Island invitation declined.");
    }

    private void leave(Player player) {
        Optional<Island> island = islandManager.getIsland(
                player.getUniqueId());

        try {
            islandManager.leaveIsland(
                    player);

            player.sendMessage(
                    "§aYou left the island.");

            island.ifPresent(
                    value -> notifyIsland(
                            value,
                            "§e"
                                    + player.getName()
                                    + " §7left the island.",
                            player));

        } catch (Exception exception) {

            sendException(
                    player,
                    exception);
        }
    }

    private void kick(
            Player player,
            String[] args) {
        Player target = requireOnlineTarget(
                player,
                args,
                "kick");

        if (target == null) {
            return;
        }

        try {
            islandManager.kickMember(
                    player,
                    target);

            player.sendMessage(
                    "§aRemoved §f"
                            + target.getName()
                            + " §afrom the island.");

            target.sendMessage(
                    "§cYou were removed from your island.");

        } catch (Exception exception) {

            sendException(
                    player,
                    exception);
        }
    }

    private void promote(
            Player player,
            String[] args) {
        Player target = requireOnlineTarget(
                player,
                args,
                "promote");

        if (target == null) {
            return;
        }

        try {
            islandManager.promoteMember(
                    player,
                    target);

            player.sendMessage(
                    "§aPromoted §f"
                            + target.getName()
                            + " §ato Co-Owner.");

            target.sendMessage(
                    "§aYou are now a Co-Owner of your island.");

        } catch (Exception exception) {

            sendException(
                    player,
                    exception);
        }
    }

    private void demote(
            Player player,
            String[] args) {
        Player target = requireOnlineTarget(
                player,
                args,
                "demote");

        if (target == null) {
            return;
        }

        try {
            islandManager.demoteMember(
                    player,
                    target);

            player.sendMessage(
                    "§aDemoted §f"
                            + target.getName()
                            + " §ato Member.");

            target.sendMessage(
                    "§eYou are now a Member of your island.");

        } catch (Exception exception) {

            sendException(
                    player,
                    exception);
        }
    }

    private Player requireOnlineTarget(
            Player sender,
            String[] args,
            String command) {
        if (args.length < 2) {

            sender.sendMessage(
                    "§cUsage: /island "
                            + command
                            + " <player>");

            return null;
        }

        Player target = Bukkit.getPlayerExact(
                args[1]);

        if (target == null) {

            sender.sendMessage(
                    "§cThat player must be online.");

            return null;
        }

        return target;
    }

    private void notifyIsland(
            Island island,
            String message,
            Player except) {
        for (IslandMember member : islandManager.getMembers(island)) {

            Player online = Bukkit.getPlayer(
                    member.getPlayerUuid());

            if (online == null) {
                continue;
            }

            if (except != null
                    && online.getUniqueId()
                            .equals(
                                    except.getUniqueId())) {

                continue;
            }

            online.sendMessage(
                    message);
        }
    }

    private void sendException(
            Player player,
            Exception exception) {
        String message = exception.getMessage();

        if (message == null
                || message.isBlank()) {

            message = "Something went wrong.";
        }

        player.sendMessage(
                "§c" + message);
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

        player.sendMessage(
                "§e/island members §7- View island members");

        player.sendMessage(
                "§e/island invite <player> §7- Invite a player");

        player.sendMessage(
                "§e/island accept §7- Accept an invitation");

        player.sendMessage(
                "§e/island decline §7- Decline an invitation");

        player.sendMessage(
                "§e/island leave §7- Leave your island");

        player.sendMessage(
                "§e/island kick <player> §7- Remove a member");

        player.sendMessage(
                "§e/island promote <player> §7- Promote a member");

        player.sendMessage(
                "§e/island demote <player> §7- Demote a Co-Owner");
    }
}