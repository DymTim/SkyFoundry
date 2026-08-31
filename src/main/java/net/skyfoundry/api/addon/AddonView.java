package net.skyfoundry.api.addon;

public record AddonView(
        String name,
        String version,
        int apiVersion,
        AddonState state) {
}
