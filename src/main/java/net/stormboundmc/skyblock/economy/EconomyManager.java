package net.stormboundmc.skyblock.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.stormboundmc.skyblock.StormboundSkyblock;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class EconomyManager {

    private final StormboundSkyblock plugin;
    private Economy economy;

    public EconomyManager(StormboundSkyblock plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault was not found. Paid island upgrades will be unavailable.");
            return false;
        }

        RegisteredServiceProvider<Economy> registration = plugin.getServer()
                .getServicesManager()
                .getRegistration(Economy.class);

        if (registration == null) {
            plugin.getLogger().warning("Vault is installed, but no economy provider is registered. Paid island upgrades will be unavailable.");
            return false;
        }

        economy = registration.getProvider();
        plugin.getLogger().info("Hooked into Vault economy provider: " + economy.getName());
        return true;
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public boolean has(Player player, double amount) {
        return amount <= 0.0D || (economy != null && economy.has(player, amount));
    }

    public boolean withdraw(Player player, double amount) {
        if (amount <= 0.0D) {
            return true;
        }
        if (economy == null) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    public void refund(Player player, double amount) {
        if (amount <= 0.0D || economy == null) {
            return;
        }
        EconomyResponse response = economy.depositPlayer(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().severe("Failed to refund " + player.getName() + " " + amount + " after an island upgrade save failure.");
        }
    }

    public String format(double amount) {
        if (economy != null) {
            return economy.format(amount);
        }
        return String.format("$%,.2f", amount);
    }
}
