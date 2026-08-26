package net.skyfoundry.core.reset;

import net.skyfoundry.core.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class PlayerResetRepository {

    private final DatabaseManager databaseManager;

    public PlayerResetRepository(
            DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public int getUsedResets(UUID playerUuid) {
        String sql = """
                SELECT resets_used
                FROM player_reset_usage
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
                    return 0;
                }

                return results.getInt(
                        "resets_used");
            }

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not load reset usage.",
                    exception);
        }
    }

    public void incrementUsedResets(
            UUID playerUuid) {
        String sql = """
                INSERT INTO player_reset_usage (
                    player_uuid,
                    resets_used
                )
                VALUES (?, 1)
                ON CONFLICT(player_uuid)
                DO UPDATE SET
                    resets_used = resets_used + 1;
                """;

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setString(
                    1,
                    playerUuid.toString());

            statement.executeUpdate();

        } catch (SQLException exception) {

            throw new IllegalStateException(
                    "Could not update reset usage.",
                    exception);
        }
    }
}