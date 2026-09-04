package net.stormboundmc.skyblock.island;

import org.bukkit.World;

public enum IslandDimension {
    OVERWORLD("overworld", World.Environment.NORMAL),
    NETHER("nether", World.Environment.NETHER),
    END("end", World.Environment.THE_END);

    private final String configKey;
    private final World.Environment environment;

    IslandDimension(String configKey, World.Environment environment) {
        this.configKey = configKey;
        this.environment = environment;
    }

    public String getConfigKey() { return configKey; }
    public World.Environment getEnvironment() { return environment; }
}
