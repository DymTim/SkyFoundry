package net.stormboundmc.skyblock.api;

import java.util.Objects;

import net.stormboundmc.skyblock.api.addon.AddonAPI;
import net.stormboundmc.skyblock.api.island.IslandAPI;

public final class StormboundAPI {

    private static volatile StormboundAPI instance;

    private final String version;
    private final IslandAPI islandAPI;
    private final AddonAPI addonAPI;

    public StormboundAPI(
            String version,
            IslandAPI islandAPI,
            AddonAPI addonAPI) {
        this.version = Objects.requireNonNull(version, "version");
        this.islandAPI = Objects.requireNonNull(islandAPI, "islandAPI");
        this.addonAPI = Objects.requireNonNull(addonAPI, "addonAPI");
    }

    public static StormboundAPI get() {
        StormboundAPI api = instance;

        if (api == null) {
            throw new IllegalStateException("Stormbound API is not initialized.");
        }

        return api;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    public static void initialize(StormboundAPI api) {
        if (instance != null) {
            throw new IllegalStateException("Stormbound API is already initialized.");
        }

        instance = Objects.requireNonNull(api, "api");
    }

    public static void shutdown() {
        instance = null;
    }

    public String version() {
        return version;
    }

    public IslandAPI islands() {
        return islandAPI;
    }

    public AddonAPI addons() {
        return addonAPI;
    }
}
