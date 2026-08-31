package net.skyfoundry.addon;

import net.skyfoundry.api.addon.AddonDescription;
import net.skyfoundry.api.addon.AddonState;
import net.skyfoundry.api.addon.SkyFoundryAddon;

import java.io.File;

final class LoadedAddon {

    private final File file;
    private final AddonDescription description;

    private AddonClassLoader classLoader;
    private SkyFoundryAddon instance;
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

    SkyFoundryAddon instance() {
        return instance;
    }

    void instance(SkyFoundryAddon instance) {
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
