package net.skyfoundry.core.progression.listener;

import net.skyfoundry.core.island.Island;
import net.skyfoundry.core.progression.BlockValueRegistry;
import net.skyfoundry.core.progression.IslandProgressionRepository;
import net.skyfoundry.core.progression.mission.DailyMissionService;
import net.skyfoundry.core.progression.mission.MissionActionType;
import net.skyfoundry.core.protection.IslandProtectionService;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Optional;

public final class IslandBlockProgressionListener
        implements Listener {

    private final IslandProtectionService protectionService;

    private final BlockValueRegistry blockValueRegistry;

    private final IslandProgressionRepository progressionRepository;

    private final DailyMissionService dailyMissionService;

    public IslandBlockProgressionListener(
            IslandProtectionService protectionService,
            BlockValueRegistry blockValueRegistry,
            IslandProgressionRepository progressionRepository,
            DailyMissionService dailyMissionService) {
        this.protectionService = protectionService;

        this.blockValueRegistry = blockValueRegistry;

        this.progressionRepository = progressionRepository;

        this.dailyMissionService = dailyMissionService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(
            BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();

        Optional<Island> island = protectionService.getIslandAt(
                block.getLocation());

        if (island.isEmpty()) {
            return;
        }

        if (!protectionService.isMemberOfIsland(
                event.getPlayer(),
                island.get())) {
            return;
        }

        long value = blockValueRegistry.getValue(
                block);

        if (value <= 0) {
            return;
        }

        progressionRepository.addBlock(
                island.get().getId(),
                block.getType()
                        .getKey()
                        .toString()
                        .toLowerCase(),
                value);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(
            BlockBreakEvent event) {
        Block block = event.getBlock();

        Optional<Island> island = protectionService.getIslandAt(
                block.getLocation());

        if (island.isEmpty()) {
            return;
        }

        if (!protectionService.isMemberOfIsland(
                event.getPlayer(),
                island.get())) {
            return;
        }

        String blockKey = block.getType()
                .getKey()
                .toString()
                .toLowerCase();

        long value = blockValueRegistry.getValue(
                block);

        if (value > 0) {
            progressionRepository.removeBlock(
                    island.get().getId(),
                    blockKey,
                    value);
        }

        dailyMissionService.progress(
                island.get(),
                MissionActionType.BREAK_BLOCK,
                blockKey);
    }
}