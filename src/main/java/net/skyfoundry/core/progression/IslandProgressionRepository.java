package net.skyfoundry.core.progression;

import net.skyfoundry.core.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class IslandProgressionRepository {

    private final DatabaseManager databaseManager;

    public IslandProgressionRepository(
            DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void updateIslandSize(
            long islandId,
            int newSize) {
        String sql = """
                UPDATE islands
                SET size = ?
                WHERE id = ?;
                """;

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setInt(
                    1,
                    newSize);

            statement.setLong(
                    2,
                    islandId);

            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                        "Island size update affected an unexpected number of rows.");
            }

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not update island size.",
                    exception);
        }
    }
}