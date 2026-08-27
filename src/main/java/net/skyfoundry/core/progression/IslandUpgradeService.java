package net.skyfoundry.core.progression;

import net.skyfoundry.core.config.ConfigManager;
import net.skyfoundry.core.island.Island;
import net.skyfoundry.core.island.IslandManager;
import net.skyfoundry.core.island.IslandMember;
import net.skyfoundry.core.island.IslandRole;
import net.skyfoundry.core.protection.IslandProtectionService;
import org.bukkit.entity.Player;

public final class IslandUpgradeService {

    private final ConfigManager configManager;

    private final IslandManager islandManager;

    private final IslandProgressionRepository progressionRepository;

    private final IslandProtectionService protectionService;

    public IslandUpgradeService(
            ConfigManager configManager,
            IslandManager islandManager,
            IslandProgressionRepository progressionRepository,
            IslandProtectionService protectionService) {
        this.configManager = configManager;
        this.islandManager = islandManager;
        this.progressionRepository = progressionRepository;
        this.protectionService = protectionService;
    }

    public IslandUpgradeResult upgrade(
            Player player) {
        Island island = islandManager.getIsland(
                player.getUniqueId()).orElseThrow(
                        () -> new IllegalStateException(
                                "You do not belong to an island."));

        IslandMember member = islandManager.getMember(
                player.getUniqueId()).orElseThrow(
                        () -> new IllegalStateException(
                                "You do not belong to an island."));

        if (configManager.isUpgradeManagementRoleRequired()
                && member.getRole() == IslandRole.MEMBER) {

            throw new IllegalStateException(
                    "Only the Owner or a Co-Owner can upgrade the island.");
        }

        int previousSize = island.getSize();

        int maximumSize = configManager.getMaximumIslandSize();

        if (previousSize >= maximumSize) {
            throw new IllegalStateException(
                    "Your island is already at the maximum size.");
        }

        int increment = configManager.getIslandUpgradeAmount();

        int newSize = Math.min(
                previousSize + increment,
                maximumSize);

        progressionRepository.updateIslandSize(
                island.getId(),
                newSize);

        /*
         * Update this instance immediately so
         * anything using it during this command
         * sees the new boundary.
         */
        island.setSize(
                newSize);

        /*
         * Protection's spatial index contains
         * island boundary information, so rebuild
         * it after a size change.
         *
         * Size upgrades are rare, so a full rebuild
         * is perfectly acceptable here.
         */
        protectionService.rebuildIndexes();

        return new IslandUpgradeResult(
                previousSize,
                newSize,
                maximumSize);
    }
}