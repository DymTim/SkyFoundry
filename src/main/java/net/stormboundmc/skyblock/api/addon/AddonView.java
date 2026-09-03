package net.stormboundmc.skyblock.api.addon;

public record AddonView(
                String name,
                String version,
                int apiVersion,
                AddonState state) {
}
