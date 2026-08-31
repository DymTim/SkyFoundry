package net.skyfoundry.island;

import org.bukkit.Location;

import java.util.UUID;

public final class IslandMember {

    private final UUID playerUuid;
    private IslandRole role;
    private Location home;

    public IslandMember(
            UUID playerUuid,
            IslandRole role,
            Location home) {
        this.playerUuid = playerUuid;
        this.role = role;
        this.home = home.clone();
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public IslandRole getRole() {
        return role;
    }

    public void setRole(
            IslandRole role) {
        this.role = role;
    }

    public Location getHome() {
        return home.clone();
    }

    public void setHome(
            Location home) {
        this.home = home.clone();
    }
}