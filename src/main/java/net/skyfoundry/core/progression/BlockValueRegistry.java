package net.skyfoundry.core.progression;

import net.skyfoundry.core.config.ConfigManager;
import org.bukkit.block.Block;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class BlockValueRegistry {

    private final ConfigManager configManager;

    private final Map<String, Long> values = new HashMap<>();

    public BlockValueRegistry(
            ConfigManager configManager) {
        this.configManager = configManager;

        reload();
    }

    public void reload() {
        values.clear();

        values.putAll(
                configManager.getBlockValues());
    }

    public long getValue(
            Block block) {
        String key = block
                .getType()
                .getKey()
                .toString()
                .toLowerCase();

        return values.getOrDefault(
                key,
                0L);
    }

    public Map<String, Long> getValues() {
        return Collections.unmodifiableMap(
                values);
    }
}