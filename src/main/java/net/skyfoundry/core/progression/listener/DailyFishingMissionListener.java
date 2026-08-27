package net.skyfoundry.core.progression.listener;

import net.skyfoundry.core.island.Island;
import net.skyfoundry.core.progression.mission.DailyMissionService;
import net.skyfoundry.core.progression.mission.MissionActionType;
import net.skyfoundry.core.protection.IslandProtectionService;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.Optional;

public final class DailyFishingMissionListener
        implements Listener {

    private final IslandProtectionService protectionService;

    private final DailyMissionService dailyMissionService;

    public DailyFishingMissionListener(
            IslandProtectionService protectionService,
            DailyMissionService dailyMissionService) {
        this.protectionService = protectionService;

        this.dailyMissionService = dailyMissionService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(
            PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {

            return;
        }

        if (!(event.getCaught() instanceof Item item)) {

            return;
        }

        Optional<Island> island = protectionService.getIslandAt(
                event.getPlayer()
                        .getLocation());

        if (island.isEmpty()) {
            return;
        }

        if (!protectionService.isMemberOfIsland(
                event.getPlayer(),
                island.get())) {
            return;
        }

        String itemKey = item.getItemStack()
                .getType()
                .getKey()
                .toString()
                .toLowerCase();

        dailyMissionService.progress(
                island.get(),
                MissionActionType.FISH,
                itemKey);
    }
}