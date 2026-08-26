package net.skyfoundry.core.island;

public enum IslandRole {

    OWNER(3),
    CO_OWNER(2),
    MEMBER(1);

    private final int weight;

    IslandRole(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isAtLeast(IslandRole role) {
        return weight >= role.weight;
    }

    public boolean canInvite() {
        return this == OWNER || this == CO_OWNER;
    }

    public boolean canKick() {
        return this == OWNER || this == CO_OWNER;
    }

    public boolean canPromote() {
        return this == OWNER;
    }

    public boolean canDemote() {
        return this == OWNER;
    }

    public boolean canDeleteIsland() {
        return this == OWNER;
    }

    public boolean canTransferOwnership() {
        return this == OWNER;
    }

    public String getDisplayName() {
        return switch (this) {
            case OWNER -> "Owner";
            case CO_OWNER -> "Co-Owner";
            case MEMBER -> "Member";
        };
    }
}