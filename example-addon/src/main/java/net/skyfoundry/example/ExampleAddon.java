package net.skyfoundry.example;

import net.skyfoundry.api.addon.SkyFoundryAddon;

public final class ExampleAddon extends SkyFoundryAddon {

    @Override
    public void onLoad() {
        getContext().logger().info("Example addon loaded.");
    }

    @Override
    public void onEnable() {
        int islandCount = getContext().api().islands().getIslandCount();

        getContext().logger().info(
                "Example addon enabled. SkyFoundry currently has "
                        + islandCount
                        + " island(s).");
    }

    @Override
    public void onDisable() {
        getContext().logger().info("Example addon disabled.");
    }
}
