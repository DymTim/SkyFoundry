package net.skyfoundry.core.progression.mission;

import net.skyfoundry.core.config.ConfigManager;
import net.skyfoundry.core.island.Island;
import net.skyfoundry.core.island.IslandManager;
import net.skyfoundry.core.progression.IslandLevelService;
import net.skyfoundry.core.progression.IslandProgress;
import net.skyfoundry.core.progression.IslandProgressionRepository;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.List;

public final class DailyMissionService {

    private final ConfigManager configManager;

    private final IslandManager islandManager;

    private final IslandProgressionRepository progressionRepository;

    private final IslandLevelService levelService;

    private final DailyMissionRegistry registry;

    private final DailyMissionGenerator generator;

    public DailyMissionService(
            ConfigManager configManager,
            IslandManager islandManager,
            IslandProgressionRepository progressionRepository,
            IslandLevelService levelService,
            DailyMissionRegistry registry,
            DailyMissionGenerator generator) {
        this.configManager = configManager;

        this.islandManager = islandManager;

        this.progressionRepository = progressionRepository;

        this.levelService = levelService;

        this.registry = registry;

        this.generator = generator;
    }

    public List<ActiveDailyMission> getToday(
            Island island) {
        LocalDate today = LocalDate.now();

        List<ActiveDailyMission> existing = progressionRepository
                .getDailyMissions(
                        island.getId(),
                        today);

        if (!existing.isEmpty()) {
            return existing;
        }

        generateToday(
                island,
                today);

        return progressionRepository
                .getDailyMissions(
                        island.getId(),
                        today);
    }

    private void generateToday(
            Island island,
            LocalDate today) {
        IslandProgress progress = progressionRepository
                .getProgress(
                        island.getId());

        int islandLevel = levelService.calculateLevel(
                progress);

        List<DailyMissionGenerator.GeneratedMission> generated = generator.generate(
                islandLevel,
                configManager
                        .getDailyMissionCount(),
                registry);

        for (DailyMissionGenerator.GeneratedMission mission : generated) {

            progressionRepository.createDailyMission(
                    island.getId(),
                    today,
                    mission.slot(),
                    mission.definition().id(),
                    mission.target(),
                    mission.definition().xpReward());
        }
    }

    public void progress(
            Island island,
            MissionActionType actionType,
            String targetKey) {
        List<ActiveDailyMission> missions = getToday(
                island);

        for (ActiveDailyMission mission : missions) {

            if (mission.completed()) {
                continue;
            }

            DailyMissionDefinition definition = registry.get(
                    mission.missionId());

            if (definition == null) {
                continue;
            }

            if (definition.actionType() != actionType) {

                continue;
            }

            if (!definition.targets()
                    .contains(
                            targetKey.toLowerCase())) {

                continue;
            }

            /*
             * Capture level BEFORE XP is awarded.
             */
            IslandProgress beforeProgress = progressionRepository
                    .getProgress(
                            island.getId());

            int previousLevel = levelService.calculateLevel(
                    beforeProgress);

            IslandProgressionRepository.MissionProgressResult result = progressionRepository
                    .addMissionProgress(
                            island.getId(),
                            mission.date(),
                            mission.slot(),
                            1);

            if (!result.completedNow()) {
                continue;
            }

            announceCompletion(
                    island,
                    definition,
                    result.awardedXp());

            /*
             * XP has now been awarded.
             */
            IslandProgress afterProgress = progressionRepository
                    .getProgress(
                            island.getId());

            int newLevel = levelService.calculateLevel(
                    afterProgress);

            if (newLevel > previousLevel) {
                announceLevelUp(
                        island,
                        previousLevel,
                        newLevel);
            }
        }
    }

    private void announceCompletion(
            Island island,
            DailyMissionDefinition definition,
            long xp) {
        for (var member : islandManager.getMembers(
                island)) {

            Player player = Bukkit.getPlayer(
                    member.getPlayerUuid());

            if (player == null) {
                continue;
            }

            player.sendMessage(
                    "§6§lDAILY MISSION COMPLETE");

            player.sendMessage(
                    "§e"
                            + definition.name());

            player.sendMessage(
                    "§a+"
                            + xp
                            + " Island XP");
        }
    }

    private void announceLevelUp(
            Island island,
            int previousLevel,
            int newLevel) {
        for (var member : islandManager.getMembers(
                island)) {

            Player player = Bukkit.getPlayer(
                    member.getPlayerUuid());

            if (player == null) {
                continue;
            }

            player.sendMessage(
                    "");

            player.sendMessage(
                    "§6§l⚙ ISLAND LEVEL UP!");

            player.sendMessage(
                    "§7Your island reached §eLevel "
                            + newLevel
                            + "§7!");

            if (newLevel > previousLevel + 1) {

                player.sendMessage(
                        "§7Levels gained: §f"
                                + previousLevel
                                + " §8→ §e"
                                + newLevel);
            }

            player.sendMessage(
                    "");
        }
    }
}