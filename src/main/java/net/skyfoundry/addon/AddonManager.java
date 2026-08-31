package net.skyfoundry.addon;

import net.skyfoundry.SkyFoundry;
import net.skyfoundry.api.addon.AddonDescription;
import net.skyfoundry.api.addon.AddonState;
import net.skyfoundry.api.addon.SkyFoundryAddon;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class AddonManager {

    public static final int API_VERSION = 1;

    private final SkyFoundry plugin;
    private final File addonsFolder;
    private final Map<String, LoadedAddon> addons = new LinkedHashMap<>();

    public AddonManager(SkyFoundry plugin) {
        this.plugin = plugin;

        String configuredFolder = plugin.getConfig().getString(
                "addons.folder",
                "addons");

        if (configuredFolder == null || configuredFolder.isBlank()) {
            configuredFolder = "addons";
        }

        this.addonsFolder = new File(plugin.getDataFolder(), configuredFolder);
    }

    public void loadAndEnableAddons() {
        if (!plugin.getConfig().getBoolean("addons.enabled", true)) {
            plugin.getLogger().info("SkyFoundry addon loading is disabled.");
            return;
        }

        ensureAddonsFolder();
        discoverAddons();

        List<LoadedAddon> ordered = resolveLoadOrder();

        for (LoadedAddon addon : ordered) {
            if (addon.state() == AddonState.FAILED) {
                continue;
            }

            loadAddon(addon);
        }

        for (LoadedAddon addon : ordered) {
            if (addon.state() != AddonState.LOADED) {
                continue;
            }

            enableAddon(addon);
        }

        long enabled = addons.values()
                .stream()
                .filter(addon -> addon.state() == AddonState.ENABLED)
                .count();

        plugin.getLogger().info(
                "Loaded " + enabled + " SkyFoundry addon(s).");
    }

    public void disableAddons() {
        List<LoadedAddon> reverse = new ArrayList<>(addons.values());
        reverse.sort(Comparator.comparing(addon -> addon.description().name(), String.CASE_INSENSITIVE_ORDER));

        for (int index = reverse.size() - 1; index >= 0; index--) {
            disableAddon(reverse.get(index));
        }

        addons.clear();
    }

    Optional<LoadedAddon> getLoadedAddon(String name) {
        if (name == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(addons.get(normalize(name)));
    }

    Collection<LoadedAddon> getLoadedAddons() {
        return List.copyOf(addons.values());
    }

    private void ensureAddonsFolder() {
        if (!addonsFolder.exists() && !addonsFolder.mkdirs()) {
            throw new IllegalStateException(
                    "Could not create addons folder: " + addonsFolder.getAbsolutePath());
        }
    }

    private void discoverAddons() {
        addons.clear();

        File[] files = addonsFolder.listFiles(
                file -> file.isFile() && file.getName().toLowerCase(Locale.ROOT).endsWith(".jar"));

        if (files == null || files.length == 0) {
            return;
        }

        List<File> jars = new ArrayList<>(List.of(files));
        jars.sort(Comparator.comparing(
                (File file) -> file.getName(),
                String.CASE_INSENSITIVE_ORDER));

        for (File file : jars) {
            try {
                AddonDescription description = readDescription(file);
                String key = normalize(description.name());

                if (addons.containsKey(key)) {
                    plugin.getLogger().severe(
                            "Duplicate SkyFoundry addon name '" + description.name() + "' in " + file.getName());
                    continue;
                }

                if (description.apiVersion() > API_VERSION) {
                    plugin.getLogger().severe(
                            "Addon '" + description.name() + "' requires addon API "
                                    + description.apiVersion() + " but this server supports " + API_VERSION + ".");
                    continue;
                }

                addons.put(key, new LoadedAddon(file, description));

            } catch (Exception exception) {
                plugin.getLogger().severe(
                        "Could not read addon '" + file.getName() + "': " + exception.getMessage());
            }
        }
    }

    private AddonDescription readDescription(File file) throws IOException {
        try (JarFile jar = new JarFile(file)) {
            JarEntry entry = jar.getJarEntry("addon.yml");

            if (entry == null) {
                throw new IllegalArgumentException("Missing addon.yml");
            }

            YamlConfiguration yaml;

            try (InputStreamReader reader = new InputStreamReader(
                    jar.getInputStream(entry),
                    StandardCharsets.UTF_8)) {
                yaml = YamlConfiguration.loadConfiguration(reader);
            }

            String name = yaml.getString("name");
            String version = yaml.getString("version");
            String main = yaml.getString("main");
            int apiVersion = yaml.getInt("api-version", 1);

            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("addon.yml is missing name");
            }

            if (version == null || version.isBlank()) {
                throw new IllegalArgumentException("addon.yml is missing version");
            }

            if (main == null || main.isBlank()) {
                throw new IllegalArgumentException("addon.yml is missing main");
            }

            return new AddonDescription(
                    name,
                    version,
                    main,
                    apiVersion,
                    yaml.getStringList("depend"),
                    yaml.getStringList("softdepend"));
        }
    }

    private List<LoadedAddon> resolveLoadOrder() {
        List<LoadedAddon> ordered = new ArrayList<>();
        Map<String, VisitState> states = new HashMap<>();

        for (LoadedAddon addon : addons.values()) {
            visit(addon, states, ordered, new ArrayList<>());
        }

        return ordered;
    }

    private void visit(
            LoadedAddon addon,
            Map<String, VisitState> states,
            List<LoadedAddon> ordered,
            List<String> path) {
        String key = normalize(addon.description().name());
        VisitState state = states.get(key);

        if (state == VisitState.VISITED || addon.state() == AddonState.FAILED) {
            return;
        }

        if (state == VisitState.VISITING) {
            path.add(addon.description().name());
            fail(addon, "Circular addon dependency: " + String.join(" -> ", path));
            return;
        }

        states.put(key, VisitState.VISITING);
        path.add(addon.description().name());

        for (String dependencyName : addon.description().depend()) {
            LoadedAddon dependency = addons.get(normalize(dependencyName));

            if (dependency == null) {
                fail(addon, "Missing required addon dependency '" + dependencyName + "'.");
                continue;
            }

            visit(dependency, states, ordered, new ArrayList<>(path));

            if (dependency.state() == AddonState.FAILED) {
                fail(addon, "Required addon dependency '" + dependencyName + "' failed to load.");
            }
        }

        for (String dependencyName : addon.description().softDepend()) {
            LoadedAddon dependency = addons.get(normalize(dependencyName));

            if (dependency != null
                    && states.get(normalize(dependencyName)) != VisitState.VISITING) {
                visit(dependency, states, ordered, new ArrayList<>(path));
            }
        }

        states.put(key, VisitState.VISITED);

        if (!ordered.contains(addon)) {
            ordered.add(addon);
        }
    }

    private void loadAddon(LoadedAddon addon) {
        for (String dependencyName : addon.description().depend()) {
            LoadedAddon dependency = addons.get(normalize(dependencyName));

            if (dependency == null
                    || (dependency.state() != AddonState.LOADED && dependency.state() != AddonState.ENABLED)) {
                fail(addon, "Required addon dependency '" + dependencyName + "' is not loaded.");
                return;
            }
        }

        try {
            URL jarUrl = addon.file().toURI().toURL();
            AddonClassLoader classLoader = new AddonClassLoader(
                    jarUrl,
                    plugin.getClass().getClassLoader());

            Class<?> mainClass = Class.forName(
                    addon.description().main(),
                    true,
                    classLoader);

            if (!SkyFoundryAddon.class.isAssignableFrom(mainClass)) {
                classLoader.close();
                throw new IllegalArgumentException(
                        "Main class must extend " + SkyFoundryAddon.class.getName());
            }

            SkyFoundryAddon instance = (SkyFoundryAddon) mainClass.getDeclaredConstructor().newInstance();
            File dataFolder = new File(addonsFolder, addon.description().name());
            AddonContextImpl context = new AddonContextImpl(
                    plugin,
                    addon.description(),
                    dataFolder);

            instance.initializeContext(context);

            addon.classLoader(classLoader);
            addon.instance(instance);
            addon.context(context);

            instance.onLoad();
            addon.state(AddonState.LOADED);

            plugin.getLogger().info(
                    "Loaded addon " + addon.description().name() + " v" + addon.description().version());

        } catch (Exception exception) {
            fail(addon, "Load failed: " + rootMessage(exception));
            cleanupFailedAddon(addon);
        }
    }

    private void enableAddon(LoadedAddon addon) {
        for (String dependencyName : addon.description().depend()) {
            LoadedAddon dependency = addons.get(normalize(dependencyName));

            if (dependency == null || dependency.state() != AddonState.ENABLED) {
                fail(addon, "Required addon dependency '" + dependencyName + "' is not enabled.");
                cleanupFailedAddon(addon);
                return;
            }
        }

        try {
            addon.instance().onEnable();
            addon.state(AddonState.ENABLED);

            plugin.getLogger().info(
                    "Enabled addon " + addon.description().name() + " v" + addon.description().version());

        } catch (Exception exception) {
            fail(addon, "Enable failed: " + rootMessage(exception));
            cleanupFailedAddon(addon);
        }
    }

    private void disableAddon(LoadedAddon addon) {
        try {
            if (addon.instance() != null
                    && (addon.state() == AddonState.ENABLED || addon.state() == AddonState.LOADED)) {
                addon.instance().onDisable();
            }
        } catch (Exception exception) {
            plugin.getLogger().severe(
                    "Addon '" + addon.description().name() + "' failed during disable: " + rootMessage(exception));
        } finally {
            if (addon.context() != null) {
                addon.context().cleanup();
            }

            if (addon.classLoader() != null) {
                try {
                    addon.classLoader().close();
                } catch (IOException exception) {
                    plugin.getLogger().warning(
                            "Could not close classloader for addon '" + addon.description().name() + "'.");
                }
            }

            if (addon.state() != AddonState.FAILED) {
                addon.state(AddonState.DISABLED);
            }
        }
    }

    private void cleanupFailedAddon(LoadedAddon addon) {
        if (addon.context() != null) {
            addon.context().cleanup();
        }

        if (addon.classLoader() != null) {
            try {
                addon.classLoader().close();
            } catch (IOException ignored) {
            }
        }
    }

    private void fail(LoadedAddon addon, String message) {
        addon.state(AddonState.FAILED);
        plugin.getLogger().severe(
                "Addon '" + addon.description().name() + "': " + message);
    }

    private String rootMessage(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private enum VisitState {
        VISITING,
        VISITED
    }
}
