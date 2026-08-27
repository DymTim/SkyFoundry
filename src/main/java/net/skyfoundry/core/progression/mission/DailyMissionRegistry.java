package net.skyfoundry.core.progression.mission;

import net.skyfoundry.core.config.ConfigManager;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DailyMissionRegistry {

    private final ConfigManager configManager;

    private final Map<String, DailyMissionDefinition> definitions = new LinkedHashMap<>();

    public DailyMissionRegistry(
            ConfigManager configManager) {
        this.configManager = configManager;

        reload();
    }

    public void reload() {
        definitions.clear();

        for (DailyMissionDefinition definition : configManager.getDailyMissionDefinitions()) {

            definitions.put(
                    definition.id(),
                    definition);
        }
    }

    public DailyMissionDefinition get(
            String missionId) {
        return definitions.get(
                missionId);
    }

    public Collection<DailyMissionDefinition> getAll() {
        return definitions.values();
    }
}