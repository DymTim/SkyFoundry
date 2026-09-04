package net.stormboundmc.skyblock.island;

import net.stormboundmc.skyblock.economy.EconomyManager;
import org.bukkit.entity.Player;

import java.sql.SQLException;

public final class IslandUpgradeManager {

    public enum Failure {
        NONE,
        NO_ISLAND,
        NO_PERMISSION,
        MAX_TIER,
        ECONOMY_UNAVAILABLE,
        INSUFFICIENT_FUNDS,
        PAYMENT_FAILED
    }

    public record Result(boolean success, Failure failure, int value, double cost) {
        public static Result success(int value, double cost) {
            return new Result(true, Failure.NONE, value, cost);
        }

        public static Result failure(Failure failure) {
            return new Result(false, failure, -1, 0.0D);
        }
    }

    private final IslandManager islandManager;
    private final EconomyManager economyManager;

    public IslandUpgradeManager(IslandManager islandManager, EconomyManager economyManager) {
        this.islandManager = islandManager;
        this.economyManager = economyManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public Result purchaseSize(Player player) throws SQLException {
        Island island = islandManager.getIsland(player.getUniqueId()).orElse(null);
        IslandRole role = islandManager.getRole(player.getUniqueId()).orElse(null);

        if (island == null || role == null) {
            return Result.failure(Failure.NO_ISLAND);
        }
        if (role != IslandRole.OWNER && role != IslandRole.CO_OWNER) {
            return Result.failure(Failure.NO_PERMISSION);
        }

        IslandDimension dimension = islandManager.getDimensionManager() == null
                ? IslandDimension.OVERWORLD
                : islandManager.getDimensionManager().getDimension(player.getWorld());
        if (dimension == null) dimension = IslandDimension.OVERWORLD;

        IslandUpgradeTier tier = islandManager.getNextSizeTier(island, dimension);
        if (tier == null) return Result.failure(Failure.MAX_TIER);

        IslandDimension upgradeDimension = dimension;
        return purchase(player, tier, () -> islandManager.upgradeSizeTo(player.getUniqueId(), upgradeDimension, tier.value()));
    }

    public Result purchaseMemberLimit(Player player) throws SQLException {
        Island island = islandManager.getIsland(player.getUniqueId()).orElse(null);
        IslandRole role = islandManager.getRole(player.getUniqueId()).orElse(null);

        if (island == null || role == null) {
            return Result.failure(Failure.NO_ISLAND);
        }
        if (role != IslandRole.OWNER && role != IslandRole.CO_OWNER) {
            return Result.failure(Failure.NO_PERMISSION);
        }

        IslandUpgradeTier tier = islandManager.getNextMemberLimitTier(island);
        if (tier == null) {
            return Result.failure(Failure.MAX_TIER);
        }

        return purchase(player, tier, () -> islandManager.upgradeMemberLimitTo(player.getUniqueId(), tier.value()));
    }

    private Result purchase(Player player, IslandUpgradeTier tier, UpgradeAction action) throws SQLException {
        double cost = tier.cost();

        if (cost > 0.0D && !economyManager.isAvailable()) {
            return Result.failure(Failure.ECONOMY_UNAVAILABLE);
        }
        if (!economyManager.has(player, cost)) {
            return new Result(false, Failure.INSUFFICIENT_FUNDS, tier.value(), cost);
        }
        if (!economyManager.withdraw(player, cost)) {
            return new Result(false, Failure.PAYMENT_FAILED, tier.value(), cost);
        }

        try {
            action.run();
            return Result.success(tier.value(), cost);
        } catch (SQLException exception) {
            economyManager.refund(player, cost);
            throw exception;
        }
    }

    @FunctionalInterface
    private interface UpgradeAction {
        void run() throws SQLException;
    }
}
