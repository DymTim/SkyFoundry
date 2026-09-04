package net.stormboundmc.skyblock.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.stormboundmc.skyblock.StormboundSkyblock;
import net.stormboundmc.skyblock.gui.holder.IslandSettingsMenuHolder;
import net.stormboundmc.skyblock.gui.holder.IslandTimeMenuHolder;
import net.stormboundmc.skyblock.gui.holder.IslandWeatherMenuHolder;
import net.stormboundmc.skyblock.island.Island;
import net.stormboundmc.skyblock.island.IslandManager;
import net.stormboundmc.skyblock.island.IslandRole;
import net.stormboundmc.skyblock.island.IslandSettings;
import net.stormboundmc.skyblock.island.IslandSettingsManager;
import net.stormboundmc.skyblock.island.IslandTimeMode;
import net.stormboundmc.skyblock.island.IslandWeatherMode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class IslandSettingsMenu {

    private static final int INVENTORY_SIZE = 45;

    private final StormboundSkyblock plugin;
    private final IslandManager islandManager;
    private final IslandSettingsManager settingsManager;
    private final IslandMenu islandMenu;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public IslandSettingsMenu(
            StormboundSkyblock plugin,
            IslandManager islandManager,
            IslandSettingsManager settingsManager,
            IslandMenu islandMenu
    ) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.settingsManager = settingsManager;
        this.islandMenu = islandMenu;
    }

    public void open(Player player) {
        Island island = islandManager.getIsland(player.getUniqueId()).orElse(null);
        if (island == null) {
            islandMenu.open(player);
            return;
        }

        IslandRole role = islandManager.getRole(player.getUniqueId()).orElse(IslandRole.MEMBER);
        IslandSettings settings = settingsManager.getSettings(island);
        boolean editable = role == IslandRole.OWNER || role == IslandRole.CO_OWNER;

        IslandSettingsMenuHolder holder = new IslandSettingsMenuHolder();
        Inventory inventory = Bukkit.createInventory(
                holder,
                INVENTORY_SIZE,
                parse(plugin.getConfig().getString(
                        "gui.settings.title",
                        "<gold><bold>⚙ ISLAND SETTINGS</bold></gold>"
                ))
        );
        holder.setInventory(inventory);

        fillBackground(inventory, "gui.settings.filler");
        placeAccents(inventory, "gui.settings.accents");

        setToggleButton(
                inventory,
                "gui.settings.buttons.visiting",
                11,
                Material.ENDER_EYE,
                "<yellow><bold>Island Visiting</bold></yellow>",
                List.of("<gray>Allow other players to visit this island.</gray>"),
                settings.isVisitingEnabled(),
                editable
        );

        setToggleButton(
                inventory,
                "gui.settings.buttons.member-building",
                13,
                Material.BRICKS,
                "<yellow><bold>Member Building</bold></yellow>",
                List.of("<gray>Allow regular Members to place and break blocks.</gray>"),
                settings.isMemberBuilding(),
                editable
        );

        setToggleButton(
                inventory,
                "gui.settings.buttons.member-interactions",
                15,
                Material.CHEST,
                "<yellow><bold>Member Interactions</bold></yellow>",
                List.of("<gray>Allow regular Members to use containers and machines.</gray>"),
                settings.isMemberInteractions(),
                editable
        );

        setCosmeticButton(
                inventory,
                "gui.settings.buttons.weather",
                20,
                Material.WATER_BUCKET,
                "<aqua><bold>Island Weather</bold></aqua>",
                "<gray>Current: <yellow>" + formatWeather(settings.getWeatherMode()) + "</yellow></gray>",
                player,
                "settings.cosmetics.permissions.weather",
                "stormbound.skyblock.cosmetic.weather"
        );

        setCosmeticButton(
                inventory,
                "gui.settings.buttons.time",
                22,
                Material.CLOCK,
                "<aqua><bold>Island Time</bold></aqua>",
                "<gray>Current: <yellow>" + formatTime(settings.getTimeMode()) + "</yellow></gray>",
                player,
                "settings.cosmetics.permissions.time",
                "stormbound.skyblock.cosmetic.time"
        );

        setCosmeticToggleButton(
                inventory,
                "gui.settings.buttons.border",
                24,
                Material.GLASS,
                "<aqua><bold>Island Border</bold></aqua>",
                settings.isBorderEnabled(),
                player,
                "settings.cosmetics.permissions.border",
                "stormbound.skyblock.cosmetic.border"
        );

        setSimpleButton(
                inventory,
                "gui.settings.buttons.back",
                40,
                Material.ARROW,
                "<yellow><bold>Back</bold></yellow>",
                List.of("<gray>Return to the island menu.</gray>")
        );

        setSimpleButton(
                inventory,
                "gui.settings.buttons.close",
                44,
                Material.BARRIER,
                "<red><bold>Close</bold></red>",
                List.of("<gray>Close this menu.</gray>")
        );

        player.openInventory(inventory);
    }

    public void openWeather(Player player) {
        Island island = islandManager.getIsland(player.getUniqueId()).orElse(null);
        if (island == null) {
            islandMenu.open(player);
            return;
        }

        IslandSettings settings = settingsManager.getSettings(island);
        IslandWeatherMenuHolder holder = new IslandWeatherMenuHolder();
        Inventory inventory = Bukkit.createInventory(
                holder,
                INVENTORY_SIZE,
                parse(plugin.getConfig().getString(
                        "gui.settings-weather.title",
                        "<gold><bold>⚙ ISLAND WEATHER</bold></gold>"
                ))
        );
        holder.setInventory(inventory);

        fillBackground(inventory, "gui.settings-weather.filler");
        placeAccents(inventory, "gui.settings-weather.accents");

        setChoiceButton(inventory, "gui.settings-weather.buttons.default", 20, Material.SUNFLOWER,
                "<yellow><bold>Default Weather</bold></yellow>", settings.getWeatherMode() == IslandWeatherMode.DEFAULT);
        setChoiceButton(inventory, "gui.settings-weather.buttons.clear", 22, Material.GLOWSTONE,
                "<yellow><bold>Clear</bold></yellow>", settings.getWeatherMode() == IslandWeatherMode.CLEAR);
        setChoiceButton(inventory, "gui.settings-weather.buttons.rain", 24, Material.WATER_BUCKET,
                "<aqua><bold>Rain</bold></aqua>", settings.getWeatherMode() == IslandWeatherMode.RAIN);

        setSimpleButton(inventory, "gui.settings-weather.buttons.back", 40, Material.ARROW,
                "<yellow><bold>Back</bold></yellow>", List.of("<gray>Return to island settings.</gray>"));
        setSimpleButton(inventory, "gui.settings-weather.buttons.close", 44, Material.BARRIER,
                "<red><bold>Close</bold></red>", List.of("<gray>Close this menu.</gray>"));

        player.openInventory(inventory);
    }

    public void openTime(Player player) {
        Island island = islandManager.getIsland(player.getUniqueId()).orElse(null);
        if (island == null) {
            islandMenu.open(player);
            return;
        }

        IslandSettings settings = settingsManager.getSettings(island);
        IslandTimeMenuHolder holder = new IslandTimeMenuHolder();
        Inventory inventory = Bukkit.createInventory(
                holder,
                INVENTORY_SIZE,
                parse(plugin.getConfig().getString(
                        "gui.settings-time.title",
                        "<gold><bold>⚙ ISLAND TIME</bold></gold>"
                ))
        );
        holder.setInventory(inventory);

        fillBackground(inventory, "gui.settings-time.filler");
        placeAccents(inventory, "gui.settings-time.accents");

        setChoiceButton(inventory, "gui.settings-time.buttons.default", 11, Material.CLOCK,
                "<yellow><bold>Default Time</bold></yellow>", settings.getTimeMode() == IslandTimeMode.DEFAULT);
        setChoiceButton(inventory, "gui.settings-time.buttons.sunrise", 13, Material.ORANGE_DYE,
                "<gold><bold>Sunrise</bold></gold>", settings.getTimeMode() == IslandTimeMode.SUNRISE);
        setChoiceButton(inventory, "gui.settings-time.buttons.noon", 15, Material.GLOWSTONE,
                "<yellow><bold>Noon</bold></yellow>", settings.getTimeMode() == IslandTimeMode.NOON);
        setChoiceButton(inventory, "gui.settings-time.buttons.sunset", 21, Material.RED_DYE,
                "<gold><bold>Sunset</bold></gold>", settings.getTimeMode() == IslandTimeMode.SUNSET);
        setChoiceButton(inventory, "gui.settings-time.buttons.midnight", 23, Material.BLACK_DYE,
                "<dark_aqua><bold>Midnight</bold></dark_aqua>", settings.getTimeMode() == IslandTimeMode.MIDNIGHT);

        setSimpleButton(inventory, "gui.settings-time.buttons.back", 40, Material.ARROW,
                "<yellow><bold>Back</bold></yellow>", List.of("<gray>Return to island settings.</gray>"));
        setSimpleButton(inventory, "gui.settings-time.buttons.close", 44, Material.BARRIER,
                "<red><bold>Close</bold></red>", List.of("<gray>Close this menu.</gray>"));

        player.openInventory(inventory);
    }

    private void setToggleButton(
            Inventory inventory,
            String path,
            int fallbackSlot,
            Material fallbackMaterial,
            String fallbackName,
            List<String> description,
            boolean enabled,
            boolean editable
    ) {
        List<String> lore = new ArrayList<>(getLore(path, description));
        lore.add("");
        lore.add(enabled ? "<green>Status: Enabled</green>" : "<red>Status: Disabled</red>");
        lore.add("");
        lore.add(editable ? "<gold>Click to toggle.</gold>" : "<dark_gray>View only.</dark_gray>");

        setDynamicButton(inventory, path, fallbackSlot, fallbackMaterial, fallbackName, lore);
    }

    private void setCosmeticButton(
            Inventory inventory,
            String path,
            int fallbackSlot,
            Material fallbackMaterial,
            String fallbackName,
            String statusLine,
            Player player,
            String permissionPath,
            String fallbackPermission
    ) {
        boolean editable = canEditCosmetic(player, permissionPath, fallbackPermission);
        List<String> lore = new ArrayList<>(getLore(path, List.of("<gray>Customize this island cosmetic.</gray>")));
        lore.add("");
        lore.add(statusLine);
        lore.add("");
        lore.add(editable ? "<gold>Click to configure.</gold>" : "<dark_gray>View only or locked.</dark_gray>");
        setDynamicButton(inventory, path, fallbackSlot, fallbackMaterial, fallbackName, lore);
    }

    private void setCosmeticToggleButton(
            Inventory inventory,
            String path,
            int fallbackSlot,
            Material fallbackMaterial,
            String fallbackName,
            boolean enabled,
            Player player,
            String permissionPath,
            String fallbackPermission
    ) {
        boolean editable = canEditCosmetic(player, permissionPath, fallbackPermission);
        List<String> lore = new ArrayList<>(getLore(path, List.of("<gray>Show the vanilla Minecraft border around this island.</gray>")));
        lore.add("");
        lore.add(enabled ? "<green>Status: Enabled</green>" : "<red>Status: Disabled</red>");
        lore.add("");
        lore.add(editable ? "<gold>Click to toggle.</gold>" : "<dark_gray>View only or locked.</dark_gray>");
        setDynamicButton(inventory, path, fallbackSlot, fallbackMaterial, fallbackName, lore);
    }

    private void setChoiceButton(
            Inventory inventory,
            String path,
            int fallbackSlot,
            Material fallbackMaterial,
            String fallbackName,
            boolean selected
    ) {
        List<String> lore = new ArrayList<>(getLore(path, List.of()));
        lore.add(selected ? "<green>Currently selected.</green>" : "<gold>Click to select.</gold>");
        setDynamicButton(inventory, path, fallbackSlot, fallbackMaterial, fallbackName, lore);
    }

    private boolean canEditCosmetic(Player player, String permissionPath, String fallbackPermission) {
        if (!settingsManager.canManage(player.getUniqueId())) {
            return false;
        }

        String permission = plugin.getConfig().getString(permissionPath, fallbackPermission);
        return permission != null && !permission.isBlank() && player.hasPermission(permission);
    }

    private void fillBackground(Inventory inventory, String path) {
        Material material = getMaterial(path, Material.GRAY_STAINED_GLASS_PANE);
        String name = plugin.getConfig().getString(path + ".name", " ");
        ItemStack item = createItem(material, name, List.of());

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item);
        }
    }

    private void placeAccents(Inventory inventory, String path) {
        if (!plugin.getConfig().getBoolean(path + ".enabled", true)) {
            return;
        }

        Material material = getMaterial(path, Material.ORANGE_STAINED_GLASS_PANE);
        String name = plugin.getConfig().getString(path + ".name", " ");
        ItemStack item = createItem(material, name, List.of());

        for (int slot : plugin.getConfig().getIntegerList(path + ".slots")) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, item);
            }
        }
    }

    private void setDynamicButton(
            Inventory inventory,
            String path,
            int fallbackSlot,
            Material fallbackMaterial,
            String fallbackName,
            List<String> lore
    ) {
        int slot = plugin.getConfig().getInt(path + ".slot", fallbackSlot);
        Material material = getMaterial(path, fallbackMaterial);
        String name = plugin.getConfig().getString(path + ".name", fallbackName);
        inventory.setItem(slot, createItem(material, name, lore));
    }

    private void setSimpleButton(
            Inventory inventory,
            String path,
            int fallbackSlot,
            Material fallbackMaterial,
            String fallbackName,
            List<String> fallbackLore
    ) {
        int slot = plugin.getConfig().getInt(path + ".slot", fallbackSlot);
        Material material = getMaterial(path, fallbackMaterial);
        String name = plugin.getConfig().getString(path + ".name", fallbackName);
        List<String> lore = getLore(path, fallbackLore);
        inventory.setItem(slot, createItem(material, name, lore));
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(parse(name));

        List<Component> components = new ArrayList<>();
        for (String line : lore) {
            components.add(parse(line));
        }
        meta.lore(components);
        item.setItemMeta(meta);
        return item;
    }

    private Material getMaterial(String path, Material fallback) {
        String value = plugin.getConfig().getString(path + ".material", fallback.name());
        Material material = Material.matchMaterial(value == null ? fallback.name() : value);
        return material == null ? fallback : material;
    }

    private List<String> getLore(String path, List<String> fallback) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null || !section.isList("lore")) {
            return fallback;
        }
        return section.getStringList("lore");
    }

    private Component parse(String value) {
        return miniMessage.deserialize(value == null ? "" : value);
    }

    private String formatWeather(IslandWeatherMode mode) {
        return switch (mode) {
            case DEFAULT -> "Default";
            case CLEAR -> "Clear";
            case RAIN -> "Rain";
        };
    }

    private String formatTime(IslandTimeMode mode) {
        return switch (mode) {
            case DEFAULT -> "Default";
            case SUNRISE -> "Sunrise";
            case NOON -> "Noon";
            case SUNSET -> "Sunset";
            case MIDNIGHT -> "Midnight";
        };
    }
}
