package net.stormboundmc.skyblock.addon;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.stormboundmc.skyblock.api.addon.AddonAPI;
import net.stormboundmc.skyblock.api.addon.AddonState;
import net.stormboundmc.skyblock.api.addon.AddonView;

public final class AddonAPIImpl implements AddonAPI {

    private final AddonManager addonManager;

    public AddonAPIImpl(AddonManager addonManager) {
        this.addonManager = addonManager;
    }

    @Override
    public Optional<AddonView> getAddon(String name) {
        return addonManager.getLoadedAddon(name).map(this::toView);
    }

    @Override
    public Collection<AddonView> getAddons() {
        List<AddonView> addons = addonManager.getLoadedAddons()
                .stream()
                .map(this::toView)
                .toList();

        return List.copyOf(addons);
    }

    @Override
    public boolean isLoaded(String name) {
        return getAddon(name)
                .map(addon -> addon.state() != AddonState.DISCOVERED && addon.state() != AddonState.FAILED)
                .orElse(false);
    }

    @Override
    public boolean isEnabled(String name) {
        return getAddon(name)
                .map(addon -> addon.state() == AddonState.ENABLED)
                .orElse(false);
    }

    private AddonView toView(LoadedAddon addon) {
        return new AddonView(
                addon.description().name(),
                addon.description().version(),
                addon.description().apiVersion(),
                addon.state());
    }
}
