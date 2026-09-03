package net.stormboundmc.skyblock.api.addon;

import java.util.Objects;

public abstract class StormboundAddon {

    private AddonContext context;

    public final AddonContext getContext() {
        AddonContext current = context;

        if (current == null) {
            throw new IllegalStateException("Addon context is not initialized.");
        }

        return current;
    }

    public final AddonDescription getDescription() {
        return getContext().description();
    }

    public final void initializeContext(AddonContext context) {
        if (this.context != null) {
            throw new IllegalStateException("Addon context is already initialized.");
        }

        this.context = Objects.requireNonNull(context, "context");
    }

    public void onLoad() throws Exception {
    }

    public void onEnable() throws Exception {
    }

    public void onDisable() throws Exception {
    }
}
