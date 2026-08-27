package net.skyfoundry.core.progression;

import net.skyfoundry.core.config.ConfigManager;

public final class IslandLevelService {

    private final ConfigManager configManager;

    public IslandLevelService(
            ConfigManager configManager) {
        this.configManager = configManager;
    }

    public long getBlockXpContribution(
            long blockScore) {
        if (blockScore <= 0) {
            return 0L;
        }

        double multiplier = configManager
                .getBlockValueContributionMultiplier();

        return Math.round(
                Math.sqrt(
                        blockScore) * multiplier);
    }

    public long getEffectiveXp(
            IslandProgress progress) {
        return progress.missionXp()
                + getBlockXpContribution(
                        progress.blockScore());
    }

    public int calculateLevel(
            long effectiveXp) {
        if (effectiveXp <= 0) {
            return 0;
        }

        double base = configManager
                .getIslandLevelBaseXp();

        double exponent = configManager
                .getIslandLevelExponent();

        double level = Math.pow(
                effectiveXp / base,
                1.0 / exponent);

        return Math.max(
                0,
                (int) Math.floor(
                        level));
    }

    public long getXpForLevel(
            int level) {
        if (level <= 0) {
            return 0L;
        }

        return Math.round(
                configManager
                        .getIslandLevelBaseXp()
                        * Math.pow(
                                level,
                                configManager
                                        .getIslandLevelExponent()));
    }

    public long getXpForNextLevel(
            int currentLevel) {
        return getXpForLevel(
                currentLevel + 1);
    }
}