package net.skyfoundry.core.island;

public interface IslandRepositoryListener {

    default void onIslandCreated(
            Island island) {
    }

    default void onIslandDeleted(
            Island island) {
    }

    default void onMemberAdded(
            IslandMember member) {
    }

    default void onMemberRemoved(
            IslandMember member) {
    }

    default void onMemberRoleChanged(
            IslandMember member) {
    }
}