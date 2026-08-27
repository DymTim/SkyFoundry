package net.skyfoundry.core.progression;

import net.skyfoundry.core.config.ConfigManager;

public final class IslandLevelService {

    private final ConfigManager configManager;

    public IslandLevelService(
            ConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * Island level is based entirely on earned
     * mission XP.
     *
     * Block value is intentionally separate from
     * progression XP.
     */
    public long getEffectiveXp(
            IslandProgress progress) {
        return progress.missionXp();
    }

    public int calculateLevel(
            IslandProgress progress) {
        return calculateLevel(
                progress.missionXp());
    }

    public int calculateLevel(
            long missionXp) {
        if (missionXp <= 0) {
            return 0;
        }

        double base = configManager
                .getIslandLevelBaseXp();

        double exponent = configManager
                .getIslandLevelExponent();

        double level = Math.pow(
                missionXp / base,
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