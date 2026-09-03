package net.stormboundmc.skyblock.addon;

import java.io.File;

import net.stormboundmc.skyblock.api.addon.AddonDescription;
import net.stormboundmc.skyblock.api.addon.AddonState;
import net.stormboundmc.skyblock.api.addon.StormboundAddon;

final class LoadedAddon {

    private final File file;
    private final AddonDescription description;

    private AddonClassLoader classLoader;
    private StormboundAddon instance;
    private AddonContextImpl context;
    private AddonState state = AddonState.DISCOVERED;

    LoadedAddon(File file, AddonDescription description) {
        this.file = file;
        this.description = description;
    }

    File file() {
        return file;
    }

    AddonDescription description() {
        return description;
    }

    AddonClassLoader classLoader() {
        return classLoader;
    }

    void classLoader(AddonClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    StormboundAddon instance() {
        return instance;
    }

    void instance(StormboundAddon instance) {
        this.instance = instance;
    }

    AddonContextImpl context() {
        return context;
    }

    void context(AddonContextImpl context) {
        this.context = context;
    }

    AddonState state() {
        return state;
    }

    void state(AddonState state) {
        this.state = state;
    }
}
