package net.skyfoundry.core.command;

import net.skyfoundry.core.invite.IslandInvite;
import net.skyfoundry.core.island.Island;
import net.skyfoundry.core.island.IslandManager;
import net.skyfoundry.core.island.IslandMember;
import net.skyfoundry.core.progression.IslandLevelService;
import net.skyfoundry.core.progression.IslandProgress;
import net.skyfoundry.core.progression.IslandProgressionRepository;
import net.skyfoundry.core.progression.IslandUpgradeResult;
import net.skyfoundry.core.progression.IslandUpgradeService;
import net.skyfoundry.core.progression.boundary.IslandBoundaryService;
import net.skyfoundry.core.progression.mission.ActiveDailyMission;
import net.skyfoundry.core.progression.mission.DailyMissionDefinition;
import net.skyfoundry.core.progression.mission.DailyMissionRegistry;
import net.skyfoundry.core.progression.mission.DailyMissionService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class IslandCommand
        implements CommandExecutor {

    private final IslandManager islandManager;

    private final IslandUpgradeService upgradeService;

    private final IslandBoundaryService boundaryService;

    private final IslandProgressionRepository progressionRepository;

    private final IslandLevelService levelService;

    private final DailyMissionService dailyMissionService;

    private final DailyMissionRegistry dailyMissionRegistry;

    public IslandCommand(
            IslandManager islandManager,
            IslandUpgradeService upgradeService,
            IslandBoundaryService boundaryService,
            IslandProgressionRepository progressionRepository,
            IslandLevelService levelService,
            DailyMissionService dailyMissionService,
            DailyMissionRegistry dailyMissionRegistry) {
        this.islandManager = islandManager;

        this.upgradeService = upgradeService;

        this.boundaryService = boundaryService;

        this.progressionRepository = progressionRepository;

        this.levelService = levelService;

        this.dailyMissionService = dailyMissionService;

        this.dailyMissionRegistry = dailyMissionRegistry;
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
            sendHelp(
                    player);

            return true;
        }

        String subcommand = args[0].toLowerCase(
                Locale.ROOT);

        switch (subcommand) {

            case "create" ->
                createIsland(
                        player);

            case "home" ->
                home(
                        player);

            case "sethome" ->
                setHome(
                        player);

            case "info" ->
                info(
                        player);

            case "members" ->
                members(
                        player);

            case "invite" ->
                invite(
                        player,
                        args);

            case "accept" ->
                accept(
                        player);

            case "decline" ->
                decline(
                        player);

            case "leave" ->
                leave(
                        player);

            case "kick" ->
                kick(
                        player,
                        args);

            case "promote" ->
                promote(
                        player,
                        args);

            case "demote" ->
                demote(
                        player,
                        args);

            case "transfer" ->
                transfer(
                        player,
                        args);

            case "delete" ->
                deleteIsland(
                        player);

            case "reset" ->
                resetIsland(
                        player);

            case "resets" ->
                resets(
                        player);

            case "confirm" ->
                confirm(
                        player);

            case "cancel" ->
                cancel(
                        player);

            case "upgrade" ->
                upgrade(
                        player);

            case "border", "boundary" ->
                border(
                        player);

            case "level" ->
                level(
                        player);

            case "value", "worth" ->
                value(
                        player);

            case "missions", "dailies" ->
                missions(
                        player);

            default ->
                sendHelp(
                        player);
        }

        return true;
    }

    private void createIsland(
            Player player) {
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

    private void home(
            Player player) {
        try {
            islandManager.teleportHome(
                    player);

            player.sendMessage(
                    "§aTeleported to your island home.");

        } catch (Exception exception) {

            sendException(
                    player,
                    exception);
        }
    }

    private void setHome(
            Player player) {
        try {
            islandManager.setHome(
                    player);

            player.sendMessage(
                    "§aYour island home has been updated.");

        } catch (Exception exception) {

            sendException(
                    player,
                    exception);
        }
    }

    private void info(
            Player player) {
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

        IslandProgress progress = progressionRepository.getProgress(
                island.getId());

        int level = levelService.calculateLevel(
                progress);

        player.sendMessage(
                "§6§lSKYFOUNDRY ISLAND");

        player.sendMessage(
                "§7Role: §f"
                        + member
                                .getRole()
                                .getDisplayName());

        player.sendMessage(
                "§7Level: §e"
                        + level);

        player.sendMessage(
                "§7Value: §f"
                        + formatNumber(
                                progress.blockScore()));

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
                                .getMembers(
                                        island)
                                .size());
    }

    private void level(
            Player player) {
        Island island = requireIsland(
                player);

        if (island == null) {
            return;
        }

        IslandProgress progress = progressionRepository.getProgress(
                island.getId());

        long missionXp = progress.missionXp();

        int level = levelService.calculateLevel(
                missionXp);

        long currentLevelXp = levelService.getXpForLevel(
                level);

        long nextLevelXp = levelService.getXpForNextLevel(
                level);

        long progressIntoLevel = missionXp
                - currentLevelXp;

        long requiredForLevel = nextLevelXp
                - currentLevelXp;

        player.sendMessage(
                "§6§lISLAND LEVEL");

        player.sendMessage(
                "§7Level: §e"
                        + level);

        player.sendMessage(
                "§7Island XP: §f"
                        + formatNumber(
                                missionXp));

        player.sendMessage(
                "§7Level Progress: §f"
                        + formatNumber(
                                progressIntoLevel)
                        + " §8/ §f"
                        + formatNumber(
                                requiredForLevel));

        player.sendMessage(
                "§7Next Level: §f"
                        + formatNumber(
                                nextLevelXp)
                        + " XP");
    }

    private void value(
            Player player) {
        Island island = requireIsland(
                player);

        if (island == null) {
            return;
        }

        IslandProgress progress = progressionRepository.getProgress(
                island.getId());

        player.sendMessage(
                "§6§lISLAND VALUE");

        player.sendMessage(
                "§7Block Value: §e"
                        + formatNumber(
                                progress.blockScore()));

        player.sendMessage(
                "§8Island value does not affect Island Level.");
    }

    private void missions(
            Player player) {
        Island island = requireIsland(
                player);

        if (island == null) {
            return;
        }

        List<ActiveDailyMission> missions = dailyMissionService.getToday(
                island);

        player.sendMessage(
                "§6§lDAILY ISLAND MISSIONS");

        if (missions.isEmpty()) {

            player.sendMessage(
                    "§cNo eligible missions could be generated.");

            return;
        }

        for (ActiveDailyMission mission : missions) {

            DailyMissionDefinition definition = dailyMissionRegistry.get(
                    mission.missionId());

            if (definition == null) {
                continue;
            }

            String status = mission.completed()
                    ? "§a✔"
                    : "§e○";

            player.sendMessage(
                    status
                            + " §f"
                            + definition.name()
                            + " §8- §7"
                            + mission.progress()
                            + "/"
                            + mission.targetAmount()
                            + " §8(§a"
                            + mission.xpReward()
                            + " XP§8)");
        }
    }

    private void members(
            Player player) {
        Island island = requireIsland(
                player);

        if (island == null) {
            return;
        }

        player.sendMessage(
                "§6§lISLAND MEMBERS");

        for (IslandMember member : islandManager.getMembers(
                island)) {

            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(
                    member.getPlayerUuid());

            String name = offlinePlayer.getName();

            if (name == null) {

                name = member
                        .getPlayerUuid()
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
                            - System.currentTimeMillis()) / 1000);

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

    private void accept(
            Player player) {
        try {
            islandManager.acceptInvite(
                    player);

            player.sendMessage(
                    "§aYou joined the island.");

            islandManager.teleportHome(
                    player);

        } catch (Exception exception) {

            sendException(
                    player,
                    exception);
        }
    }

    private void decline(
            Player player) {
        if (!islandManager.declineInvite(
                player)) {

            player.sendMessage(
                    "§cYou do not have an active island invitation.");

            return;
        }

        player.sendMessage(
                "§7Island invitation declined.");
    }

    private void leave(
            Player player) {
        try {
            islandManager.leaveIsland(
                    player);

            player.sendMessage(
                    "§aYou left the island.");

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

    private void transfer(
            Player player,
            String[] args) {
        Player target = requireOnlineTarget(
                player,
                args,
                "transfer");

        if (target == null) {
            return;
        }

        try {
            islandManager.requestOwnershipTransfer(
                    player,
                    target);

            player.sendMessage(
                    "§eYou are about to transfer island ownership to §f"
                            + target.getName()
                            + "§e.");

            player.sendMessage(
                    "§eUse §f/island confirm §eto continue or §f/island cancel §eto cancel.");

        } catch (Exception exception) {

            sendException(
                    player,
                    exception);
        }
    }

    private void deleteIsland(
            Player player) {
        try {
            islandManager.requestDeletion(
                    player);

            player.sendMessage(
                    "§c§lWARNING");

            player.sendMessage(
                    "§cThis will permanently delete your island.");

            player.sendMessage(
                    "§eUse §f/island confirm §eto continue.");

        } catch (Exception exception) {

            sendException(
                    player,
                    exception);
        }
    }

    private void resetIsland(
            Player player) {
        try {
            islandManager.requestReset(
                    player);

            player.sendMessage(
                    "§eUse §f/island confirm §eto reset your island.");

        } catch (Exception exception) {

            sendException(
                    player,
                    exception);
        }
    }

    private void resets(
            Player player) {
        player.sendMessage(
                "§6§lISLAND RESETS");

        player.sendMessage(
                "§7Used: §f"
                        + islandManager.getUsedResets(
                                player.getUniqueId()));

        player.sendMessage(
                "§7Remaining: §f"
                        + islandManager.getRemainingResets(
                                player.getUniqueId()));
    }

    private void confirm(
            Player player) {
        try {
            player.sendMessage(
                    islandManager.confirm(
                            player));

        } catch (Exception exception) {

            sendException(
                    player,
                    exception);
        }
    }

    private void cancel(
            Player player) {
        if (!islandManager.cancelConfirmation(
                player)) {

            player.sendMessage(
                    "§cYou do not have an active island confirmation.");

            return;
        }

        player.sendMessage(
                "§7Island action cancelled.");
    }

    private void upgrade(
            Player player) {
        try {
            IslandUpgradeResult result = upgradeService.upgrade(
                    player);

            player.sendMessage(
                    "§6§lISLAND UPGRADED");

            player.sendMessage(
                    "§7"
                            + result.previousSize()
                            + "x"
                            + result.previousSize()
                            + " §8→ §a"
                            + result.newSize()
                            + "x"
                            + result.newSize());

        } catch (Exception exception) {

            sendException(
                    player,
                    exception);
        }
    }

    private void border(
            Player player) {
        try {
            boundaryService.showBoundary(
                    player);

            player.sendMessage(
                    "§aShowing your island boundary.");

        } catch (Exception exception) {

            sendException(
                    player,
                    exception);
        }
    }

    private Island requireIsland(
            Player player) {
        Optional<Island> island = islandManager.getIsland(
                player.getUniqueId());

        if (island.isEmpty()) {

            player.sendMessage(
                    "§cYou do not belong to an island.");

            return null;
        }

        return island.get();
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
        }

        return target;
    }

    private String formatNumber(
            long number) {
        return NumberFormat
                .getIntegerInstance(
                        Locale.US)
                .format(
                        number);
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

    private void sendHelp(
            Player player) {
        player.sendMessage(
                "§6§lSKYFOUNDRY");

        player.sendMessage(
                "§e/island create §7- Create your island");

        player.sendMessage(
                "§e/island home §7- Return home");

        player.sendMessage(
                "§e/island sethome §7- Set your personal home");

        player.sendMessage(
                "§e/island info §7- View island information");

        player.sendMessage(
                "§e/island level §7- View Island Level and XP");

        player.sendMessage(
                "§e/island value §7- View island block value");

        player.sendMessage(
                "§e/island missions §7- View today's daily missions");

        player.sendMessage(
                "§e/island upgrade §7- Expand your island");

        player.sendMessage(
                "§e/island border §7- Show your island boundary");

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

        player.sendMessage(
                "§e/island transfer <player> §7- Transfer ownership");

        player.sendMessage(
                "§e/island reset §7- Reset your island");

        player.sendMessage(
                "§e/island delete §7- Delete your island");
    }
}