package net.skyfoundry.core.island;

import net.skyfoundry.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class IslandRepository {

    private final DatabaseManager databaseManager;

    public IslandRepository(
            DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public Optional<Island> findById(long islandId) {
        String sql = """
                SELECT *
                FROM islands
                WHERE id = ?
                LIMIT 1;
                """;

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setLong(
                    1,
                    islandId);

            try (ResultSet results = statement.executeQuery()) {

                if (!results.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        readIsland(results));
            }

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not load island " + islandId,
                    exception);
        }
    }

    public Optional<Island> findByOwner(
            UUID ownerUuid) {
        String sql = """
                SELECT *
                FROM islands
                WHERE owner_uuid = ?
                LIMIT 1;
                """;

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setString(
                    1,
                    ownerUuid.toString());

            try (ResultSet results = statement.executeQuery()) {

                if (!results.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        readIsland(results));
            }

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not load island for owner "
                            + ownerUuid,
                    exception);
        }
    }

    public Optional<Island> findByMember(
            UUID playerUuid) {
        String sql = """
                SELECT islands.*
                FROM islands
                INNER JOIN island_members
                    ON island_members.island_id = islands.id
                WHERE island_members.player_uuid = ?
                LIMIT 1;
                """;

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setString(
                    1,
                    playerUuid.toString());

            try (ResultSet results = statement.executeQuery()) {

                if (!results.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        readIsland(results));
            }

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not load island membership for "
                            + playerUuid,
                    exception);
        }
    }

    public Optional<IslandMember> findMember(
            UUID playerUuid) {
        String sql = """
                SELECT *
                FROM island_members
                WHERE player_uuid = ?
                LIMIT 1;
                """;

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setString(
                    1,
                    playerUuid.toString());

            try (ResultSet results = statement.executeQuery()) {

                if (!results.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        readMember(results));
            }

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not load island member "
                            + playerUuid,
                    exception);
        }
    }

    public Optional<IslandMember> findMember(
            long islandId,
            UUID playerUuid) {
        String sql = """
                SELECT *
                FROM island_members
                WHERE island_id = ?
                  AND player_uuid = ?
                LIMIT 1;
                """;

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setLong(
                    1,
                    islandId);

            statement.setString(
                    2,
                    playerUuid.toString());

            try (ResultSet results = statement.executeQuery()) {

                if (!results.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                        readMember(results));
            }

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not load island member.",
                    exception);
        }
    }

    public List<IslandMember> getMembers(
            long islandId) {
        String sql = """
                SELECT *
                FROM island_members
                WHERE island_id = ?
                ORDER BY
                    CASE role
                        WHEN 'OWNER' THEN 1
                        WHEN 'CO_OWNER' THEN 2
                        ELSE 3
                    END,
                    joined_at ASC;
                """;

        List<IslandMember> members = new ArrayList<>();

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setLong(
                    1,
                    islandId);

            try (ResultSet results = statement.executeQuery()) {

                while (results.next()) {
                    members.add(
                            readMember(results));
                }
            }

            return members;

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not load island members.",
                    exception);
        }
    }

    public int countMembers(long islandId) {
        String sql = """
                SELECT COUNT(*) AS member_count
                FROM island_members
                WHERE island_id = ?;
                """;

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setLong(
                    1,
                    islandId);

            try (ResultSet results = statement.executeQuery()) {

                if (results.next()) {
                    return results.getInt(
                            "member_count");
                }

                return 0;
            }

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not count island members.",
                    exception);
        }
    }

    public void addMember(
            long islandId,
            UUID playerUuid,
            IslandRole role) {
        String sql = """
                INSERT INTO island_members (
                    island_id,
                    player_uuid,
                    role,
                    joined_at
                )
                VALUES (?, ?, ?, ?);
                """;

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setLong(
                    1,
                    islandId);

            statement.setString(
                    2,
                    playerUuid.toString());

            statement.setString(
                    3,
                    role.name());

            statement.setLong(
                    4,
                    System.currentTimeMillis());

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not add island member.",
                    exception);
        }
    }

    public void removeMember(
            long islandId,
            UUID playerUuid) {
        String sql = """
                DELETE FROM island_members
                WHERE island_id = ?
                  AND player_uuid = ?;
                """;

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setLong(
                    1,
                    islandId);

            statement.setString(
                    2,
                    playerUuid.toString());

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not remove island member.",
                    exception);
        }
    }

    public void updateRole(
            long islandId,
            UUID playerUuid,
            IslandRole role) {
        String sql = """
                UPDATE island_members
                SET role = ?
                WHERE island_id = ?
                  AND player_uuid = ?;
                """;

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setString(
                    1,
                    role.name());

            statement.setLong(
                    2,
                    islandId);

            statement.setString(
                    3,
                    playerUuid.toString());

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not update island role.",
                    exception);
        }
    }

    public void transferOwnership(
            long islandId,
            UUID oldOwnerUuid,
            UUID newOwnerUuid) {
        Connection connection = databaseManager.getConnection();

        try {
            connection.setAutoCommit(false);

            String updateIsland = """
                    UPDATE islands
                    SET owner_uuid = ?
                    WHERE id = ?
                      AND owner_uuid = ?;
                    """;

            String demoteOldOwner = """
                    UPDATE island_members
                    SET role = ?
                    WHERE island_id = ?
                      AND player_uuid = ?;
                    """;

            String promoteNewOwner = """
                    UPDATE island_members
                    SET role = ?
                    WHERE island_id = ?
                      AND player_uuid = ?;
                    """;

            try (
                    PreparedStatement islandStatement = connection.prepareStatement(
                            updateIsland);

                    PreparedStatement oldOwnerStatement = connection.prepareStatement(
                            demoteOldOwner);

                    PreparedStatement newOwnerStatement = connection.prepareStatement(
                            promoteNewOwner)) {

                islandStatement.setString(
                        1,
                        newOwnerUuid.toString());

                islandStatement.setLong(
                        2,
                        islandId);

                islandStatement.setString(
                        3,
                        oldOwnerUuid.toString());

                if (islandStatement.executeUpdate() != 1) {

                    throw new SQLException(
                            "Could not update island owner.");
                }

                oldOwnerStatement.setString(
                        1,
                        IslandRole.CO_OWNER.name());

                oldOwnerStatement.setLong(
                        2,
                        islandId);

                oldOwnerStatement.setString(
                        3,
                        oldOwnerUuid.toString());

                oldOwnerStatement.executeUpdate();

                newOwnerStatement.setString(
                        1,
                        IslandRole.OWNER.name());

                newOwnerStatement.setLong(
                        2,
                        islandId);

                newOwnerStatement.setString(
                        3,
                        newOwnerUuid.toString());

                if (newOwnerStatement.executeUpdate() != 1) {

                    throw new SQLException(
                            "Could not promote new owner.");
                }

                connection.commit();

            } catch (SQLException exception) {

                connection.rollback();
                throw exception;

            } finally {

                connection.setAutoCommit(true);
            }

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not transfer island ownership.",
                    exception);
        }
    }

    public void deleteIsland(long islandId) {
        String sql = """
                DELETE FROM islands
                WHERE id = ?;
                """;

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setLong(
                    1,
                    islandId);

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not delete island.",
                    exception);
        }
    }

    public Island create(
            UUID ownerUuid,
            String worldName,
            int centerX,
            int centerY,
            int centerZ,
            int size,
            int slotIndex) {
        String sql = """
                INSERT INTO islands (
                    owner_uuid,
                    world_name,
                    center_x,
                    center_y,
                    center_z,
                    size,
                    slot_index,
                    created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?);
                """;

        long createdAt = System.currentTimeMillis();

        Connection connection = databaseManager.getConnection();

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement statement = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS)) {

                statement.setString(
                        1,
                        ownerUuid.toString());

                statement.setString(
                        2,
                        worldName);

                statement.setInt(
                        3,
                        centerX);

                statement.setInt(
                        4,
                        centerY);

                statement.setInt(
                        5,
                        centerZ);

                statement.setInt(
                        6,
                        size);

                statement.setInt(
                        7,
                        slotIndex);

                statement.setLong(
                        8,
                        createdAt);

                statement.executeUpdate();

                long islandId;

                try (ResultSet keys = statement.getGeneratedKeys()) {

                    if (!keys.next()) {
                        throw new SQLException(
                                "Island insert did not return an ID.");
                    }

                    islandId = keys.getLong(1);
                }

                addOwnerMember(
                        connection,
                        islandId,
                        ownerUuid);

                connection.commit();

                return new Island(
                        islandId,
                        ownerUuid,
                        worldName,
                        centerX,
                        centerY,
                        centerZ,
                        size,
                        slotIndex,
                        createdAt);

            } catch (SQLException exception) {

                connection.rollback();
                throw exception;

            } finally {

                connection.setAutoCommit(true);
            }

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not create island.",
                    exception);
        }
    }

    private void addOwnerMember(
            Connection connection,
            long islandId,
            UUID ownerUuid) throws SQLException {

        String sql = """
                INSERT INTO island_members (
                    island_id,
                    player_uuid,
                    role,
                    joined_at
                )
                VALUES (?, ?, ?, ?);
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(
                    1,
                    islandId);

            statement.setString(
                    2,
                    ownerUuid.toString());

            statement.setString(
                    3,
                    IslandRole.OWNER.name());

            statement.setLong(
                    4,
                    System.currentTimeMillis());

            statement.executeUpdate();
        }
    }

    public int getNextSlotIndex() {
        String sql = """
                SELECT COALESCE(MAX(slot_index), -1) + 1
                AS next_slot
                FROM islands;
                """;

        try (
                Statement statement = databaseManager
                        .getConnection()
                        .createStatement();

                ResultSet results = statement.executeQuery(sql)) {

            if (results.next()) {
                return results.getInt(
                        "next_slot");
            }

            return 0;

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not calculate next island slot.",
                    exception);
        }
    }

    public int countIslands() {
        String sql = """
                SELECT COUNT(*) AS island_count
                FROM islands;
                """;

        try (
                Statement statement = databaseManager
                        .getConnection()
                        .createStatement();

                ResultSet results = statement.executeQuery(sql)) {

            if (results.next()) {
                return results.getInt(
                        "island_count");
            }

            return 0;

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not count islands.",
                    exception);
        }
    }

    private Island readIsland(
            ResultSet results) throws SQLException {

        return new Island(
                results.getLong("id"),
                UUID.fromString(
                        results.getString(
                                "owner_uuid")),
                results.getString(
                        "world_name"),
                results.getInt(
                        "center_x"),
                results.getInt(
                        "center_y"),
                results.getInt(
                        "center_z"),
                results.getInt(
                        "size"),
                results.getInt(
                        "slot_index"),
                results.getLong(
                        "created_at"));
    }

    private IslandMember readMember(
            ResultSet results) throws SQLException {

        return new IslandMember(
                results.getLong(
                        "island_id"),
                UUID.fromString(
                        results.getString(
                                "player_uuid")),
                IslandRole.valueOf(
                        results.getString(
                                "role")),
                results.getLong(
                        "joined_at"));
    }
}