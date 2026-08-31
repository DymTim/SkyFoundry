package net.skyfoundry.api;

import net.skyfoundry.api.addon.AddonAPI;
import net.skyfoundry.api.island.IslandAPI;

import java.util.Objects;

public final class SkyFoundryAPI {

    private static volatile SkyFoundryAPI instance;

    private final String version;
    private final IslandAPI islandAPI;
    private final AddonAPI addonAPI;

    public SkyFoundryAPI(
            String version,
            IslandAPI islandAPI,
            AddonAPI addonAPI) {
        this.version = Objects.requireNonNull(version, "version");
        this.islandAPI = Objects.requireNonNull(islandAPI, "islandAPI");
        this.addonAPI = Objects.requireNonNull(addonAPI, "addonAPI");
    }

    public static SkyFoundryAPI get() {
        SkyFoundryAPI api = instance;

        if (api == null) {
            throw new IllegalStateException("SkyFoundry API is not initialized.");
        }

        return api;
    }

    public static boolean isAvailable() {
        return instance != null;
    }

    public static void initialize(SkyFoundryAPI api) {
        if (instance != null) {
            throw new IllegalStateException("SkyFoundry API is already initialized.");
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
