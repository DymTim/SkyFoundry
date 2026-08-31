package net.skyfoundry.api.addon;

import java.util.List;
import java.util.Objects;

public record AddonDescription(
        String name,
        String version,
        String main,
        int apiVersion,
        List<String> depend,
        List<String> softDepend) {

    public AddonDescription {
        name = requireText(name, "name");
        version = requireText(version, "version");
        main = requireText(main, "main");

        if (apiVersion < 1) {
            throw new IllegalArgumentException("apiVersion must be at least 1");
        }

        depend = List.copyOf(Objects.requireNonNull(depend, "depend"));
        softDepend = List.copyOf(Objects.requireNonNull(softDepend, "softDepend"));
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);

        String trimmed = value.trim();

        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }

        return trimmed;
    }
}
