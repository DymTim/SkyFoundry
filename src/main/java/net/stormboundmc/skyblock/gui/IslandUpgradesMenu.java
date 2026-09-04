package net.stormboundmc.skyblock.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.stormboundmc.skyblock.StormboundSkyblock;
import net.stormboundmc.skyblock.gui.holder.IslandUpgradesMenuHolder;
import net.stormboundmc.skyblock.island.Island;
import net.stormboundmc.skyblock.island.IslandManager;
import net.stormboundmc.skyblock.island.IslandDimension;
import net.stormboundmc.skyblock.island.IslandRole;
import net.stormboundmc.skyblock.island.IslandUpgradeManager;
import net.stormboundmc.skyblock.island.IslandUpgradeTier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class IslandUpgradesMenu {

    private final StormboundSkyblock plugin;
    private final IslandManager islandManager;
    private final IslandUpgradeManager upgradeManager;
    private final MiniMessage mini = MiniMessage.miniMessage();

    public IslandUpgradesMenu(
            StormboundSkyblock plugin,
            IslandManager islandManager,
            IslandUpgradeManager upgradeManager
    ) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.upgradeManager = upgradeManager;
    }

    public void open(Player player) {
        Island island = islandManager.getIsland(player.getUniqueId()).orElse(null);
        if (island == null) {
            return;
        }

        IslandRole role = islandManager.getRole(player.getUniqueId()).orElse(IslandRole.MEMBER);
        boolean editable = role == IslandRole.OWNER || role == IslandRole.CO_OWNER;
        IslandDimension dimension = islandManager.getDimensionManager() == null
                ? IslandDimension.OVERWORLD
                : islandManager.getDimensionManager().getDimension(player.getWorld());
        if (dimension == null) dimension = IslandDimension.OVERWORLD;

        IslandUpgradesMenuHolder holder = new IslandUpgradesMenuHolder();
        Inventory inventory = Bukkit.createInventory(
                holder,
                45,
                parse(plugin.getConfig().getString(
                        "gui.upgrades.title",
                        "<gold><bold>⚙ {dimension} UPGRADES</bold></gold>"
                ).replace("{dimension}", dimension.name()))
        );
        holder.setInventory(inventory);

        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < 45; slot++) {
            inventory.setItem(slot, filler);
        }

        Material accentMaterial = material(
                "gui.upgrades.accents.material",
                Material.ORANGE_STAINED_GLASS_PANE
        );
        ItemStack accent = item(accentMaterial, " ", List.of());
        for (int slot : plugin.getConfig().getIntegerList("gui.upgrades.accents.slots")) {
            if (slot >= 0 && slot < 45) {
                inventory.setItem(slot, accent);
            }
        }

        IslandUpgradeTier nextSize = islandManager.getNextSizeTier(island, dimension);
        inventory.setItem(
                plugin.getConfig().getInt("gui.upgrades.buttons.size.slot", 20),
                item(
                        material("gui.upgrades.buttons.size.material", Material.GRASS_BLOCK),
                        "<yellow><bold>Island Size</bold></yellow>",
                        upgradeLore(
                                islandManager.getIslandSize(island, dimension) + " x " + islandManager.getIslandSize(island, dimension),
                                nextSize == null ? null : nextSize.value() + " x " + nextSize.value(),
                                nextSize,
                                editable,
                                player
                        )
                )
        );

        IslandUpgradeTier nextMembers = islandManager.getNextMemberLimitTier(island);
        inventory.setItem(
                plugin.getConfig().getInt("gui.upgrades.buttons.member-limit.slot", 24),
                item(
                        material("gui.upgrades.buttons.member-limit.material", Material.PLAYER_HEAD),
                        "<yellow><bold>Member Limit</bold></yellow>",
                        upgradeLore(
                                island.getMemberLimit() + " members",
                                nextMembers == null ? null : nextMembers.value() + " members",
                                nextMembers,
                                editable,
                                player
                        )
                )
        );

        inventory.setItem(
                plugin.getConfig().getInt("gui.upgrades.buttons.back.slot", 40),
                item(Material.ARROW, "<yellow>Back</yellow>", List.of("<gray>Return to the island menu.</gray>"))
        );
        inventory.setItem(
                plugin.getConfig().getInt("gui.upgrades.buttons.close.slot", 44),
                item(Material.BARRIER, "<red>Close</red>", List.of())
        );

        player.openInventory(inventory);
    }

    private List<String> upgradeLore(
            String current,
            String next,
            IslandUpgradeTier tier,
            boolean editable,
            Player player
    ) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Current: <yellow>" + current + "</yellow></gray>");

        if (tier == null || next == null) {
            lore.add("");
            lore.add("<green>Maximum upgrade reached.</green>");
            return lore;
        }

        lore.add("<gray>Next: <green>" + next + "</green></gray>");
        lore.add("<gray>Cost: <gold>" + upgradeManager.getEconomyManager().format(tier.cost()) + "</gold></gray>");
        lore.add("");

        if (!editable) {
            lore.add("<dark_gray>Owner or Co-Owner required.</dark_gray>");
        } else if (tier.cost() > 0.0D && !upgradeManager.getEconomyManager().isAvailable()) {
            lore.add("<red>Economy unavailable.</red>");
        } else if (!upgradeManager.getEconomyManager().has(player, tier.cost())) {
            lore.add("<red>Not enough money.</red>");
        } else {
            lore.add("<gold>Click to upgrade.</gold>");
        }

        return lore;
    }

    private Material material(String path, Material fallback) {
        String name = plugin.getConfig().getString(path, fallback.name());
        try {
            return Material.valueOf(name.toUpperCase());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(parse(name));
        meta.lore(lore.stream().map(this::parse).toList());
        item.setItemMeta(meta);
        return item;
    }

    private Component parse(String text) {
        return mini.deserialize(text == null ? "" : text);
    }
}
