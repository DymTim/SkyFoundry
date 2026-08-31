package net.skyfoundry.island;

public enum IslandRole {

    OWNER,
    CO_OWNER,
    MEMBER;

    public boolean canBuild() {
        return true;
    }

    public boolean canInteract() {
        return true;
    }

    public boolean canInvite() {
        return this == OWNER
                || this == CO_OWNER;
    }

    public boolean canChangeRoles() {
        return this == OWNER;
    }

    public boolean canDeleteIsland() {
        return this == OWNER;
    }

    public boolean canLeaveIsland() {
        return this != OWNER;
    }
}