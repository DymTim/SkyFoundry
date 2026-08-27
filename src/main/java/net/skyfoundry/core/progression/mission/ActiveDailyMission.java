package net.skyfoundry.core.progression.mission;

import java.time.LocalDate;

public record ActiveDailyMission(
        long islandId,
        LocalDate date,
        int slot,
        String missionId,
        int targetAmount,
        int progress,
        long xpReward,
        boolean completed) {

    public int remaining() {
        return Math.max(
                0,
                targetAmount - progress);
    }

    public double progressPercent() {
        if (targetAmount <= 0) {
            return 100.0;
        }

        return Math.min(
                100.0,
                (progress
                        / (double) targetAmount) * 100.0);
    }
}