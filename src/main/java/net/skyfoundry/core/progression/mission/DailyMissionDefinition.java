package net.skyfoundry.core.progression.mission;

import java.util.List;

public record DailyMissionDefinition(
        String id,
        String name,
        MissionCategory category,
        MissionActionType actionType,
        int minimumIslandLevel,
        List<String> targets,
        int minimumAmount,
        int maximumAmount,
        int amountPerLevel,
        long xpReward) {

    public boolean isEligible(
            int islandLevel) {
        return islandLevel >= minimumIslandLevel;
    }
}