package net.skyfoundry.core.progression.listener;

import net.skyfoundry.core.island.Island;
import net.skyfoundry.core.progression.mission.DailyMissionService;
import net.skyfoundry.core.progression.mission.MissionActionType;
import net.skyfoundry.core.protection.IslandProtectionService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Optional;

public final class DailyCombatMissionListener
        implements Listener {

    private final IslandProtectionService protectionService;

    private final DailyMissionService dailyMissionService;

    public DailyCombatMissionListener(
            IslandProtectionService protectionService,
            DailyMissionService dailyMissionService) {
        this.protectionService = protectionService;

        this.dailyMissionService = dailyMissionService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(
            EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();

        if (killer == null) {
            return;
        }

        Optional<Island> island = protectionService.getIslandAt(
                event.getEntity()
                        .getLocation());

        if (island.isEmpty()) {
            return;
        }

        if (!protectionService.isMemberOfIsland(
                killer,
                island.get())) {
            return;
        }

        String entityKey = event.getEntity()
                .getType()
                .getKey()
                .toString()
                .toLowerCase();

        dailyMissionService.progress(
                island.get(),
                MissionActionType.KILL_ENTITY,
                entityKey);
    }
}