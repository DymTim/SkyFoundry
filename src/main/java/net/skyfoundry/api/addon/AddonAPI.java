package net.skyfoundry.api.addon;

import java.util.Collection;
import java.util.Optional;

public interface AddonAPI {

    Optional<AddonView> getAddon(String name);

    Collection<AddonView> getAddons();

    boolean isLoaded(String name);

    boolean isEnabled(String name);
}
