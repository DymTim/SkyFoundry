package net.skyfoundry.core.progression;

import net.skyfoundry.core.database.DatabaseManager;
import net.skyfoundry.core.progression.mission.ActiveDailyMission;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class IslandProgressionRepository {

    private final DatabaseManager databaseManager;

    public IslandProgressionRepository(
            DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public IslandProgress getProgress(
            long islandId) {
        ensureProgressRow(
                islandId);

        String sql = """
                SELECT mission_xp, block_score
                FROM island_progression
                WHERE island_id = ?
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
                    return new IslandProgress(
                            0L,
                            0L);
                }

                return new IslandProgress(
                        results.getLong(
                                "mission_xp"),
                        results.getLong(
                                "block_score"));
            }

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not load island progression.",
                    exception);
        }
    }

    public void addBlock(
            long islandId,
            String blockKey,
            long value) {
        if (value <= 0) {
            return;
        }

        Connection connection = databaseManager.getConnection();

        try {
            connection.setAutoCommit(false);

            ensureProgressRow(
                    connection,
                    islandId);

            String countSql = """
                    INSERT INTO island_block_counts (
                        island_id,
                        block_key,
                        block_count
                    )
                    VALUES (?, ?, 1)
                    ON CONFLICT(island_id, block_key)
                    DO UPDATE SET
                        block_count = block_count + 1;
                    """;

            String scoreSql = """
                    UPDATE island_progression
                    SET block_score =
                        block_score + ?
                    WHERE island_id = ?;
                    """;

            try (
                    PreparedStatement countStatement = connection.prepareStatement(
                            countSql);

                    PreparedStatement scoreStatement = connection.prepareStatement(
                            scoreSql)) {
                countStatement.setLong(
                        1,
                        islandId);

                countStatement.setString(
                        2,
                        blockKey);

                countStatement.executeUpdate();

                scoreStatement.setLong(
                        1,
                        value);

                scoreStatement.setLong(
                        2,
                        islandId);

                scoreStatement.executeUpdate();

                connection.commit();

            } catch (SQLException exception) {
                connection.rollback();
                throw exception;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not add island block value.",
                    exception);
        }
    }

    public void removeBlock(
            long islandId,
            String blockKey,
            long value) {
        if (value <= 0) {
            return;
        }

        Connection connection = databaseManager.getConnection();

        try {
            connection.setAutoCommit(false);

            int existingCount = getBlockCount(
                    connection,
                    islandId,
                    blockKey);

            /*
             * Important anti-desync safeguard:
             * Don't subtract score for blocks the
             * progression system never recorded.
             */
            if (existingCount <= 0) {
                connection.rollback();
                connection.setAutoCommit(true);
                return;
            }

            String countSql = """
                    UPDATE island_block_counts
                    SET block_count =
                        block_count - 1
                    WHERE island_id = ?
                      AND block_key = ?;
                    """;

            String scoreSql = """
                    UPDATE island_progression
                    SET block_score =
                        MAX(
                            0,
                            block_score - ?
                        )
                    WHERE island_id = ?;
                    """;

            try (
                    PreparedStatement countStatement = connection.prepareStatement(
                            countSql);

                    PreparedStatement scoreStatement = connection.prepareStatement(
                            scoreSql)) {

                countStatement.setLong(
                        1,
                        islandId);

                countStatement.setString(
                        2,
                        blockKey);

                countStatement.executeUpdate();

                scoreStatement.setLong(
                        1,
                        value);

                scoreStatement.setLong(
                        2,
                        islandId);

                scoreStatement.executeUpdate();

                connection.commit();

            } catch (SQLException exception) {
                connection.rollback();
                throw exception;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not remove island block value.",
                    exception);
        }
    }

    public List<ActiveDailyMission> getDailyMissions(
            long islandId,
            LocalDate date) {
        String sql = """
                SELECT *
                FROM island_daily_missions
                WHERE island_id = ?
                  AND mission_date = ?
                ORDER BY slot ASC;
                """;

        List<ActiveDailyMission> missions = new ArrayList<>();

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setLong(
                    1,
                    islandId);

            statement.setString(
                    2,
                    date.toString());

            try (ResultSet results = statement.executeQuery()) {

                while (results.next()) {
                    missions.add(
                            readMission(
                                    results));
                }
            }

            return missions;

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not load daily island missions.",
                    exception);
        }
    }

    public void createDailyMission(
            long islandId,
            LocalDate date,
            int slot,
            String missionId,
            int targetAmount,
            long xpReward) {
        String sql = """
                INSERT INTO island_daily_missions (
                    island_id,
                    mission_date,
                    slot,
                    mission_id,
                    target_amount,
                    progress,
                    xp_reward,
                    completed
                )
                VALUES (?, ?, ?, ?, ?, 0, ?, 0);
                """;

        try (PreparedStatement statement = databaseManager
                .getConnection()
                .prepareStatement(sql)) {

            statement.setLong(
                    1,
                    islandId);

            statement.setString(
                    2,
                    date.toString());

            statement.setInt(
                    3,
                    slot);

            statement.setString(
                    4,
                    missionId);

            statement.setInt(
                    5,
                    targetAmount);

            statement.setLong(
                    6,
                    xpReward);

            statement.executeUpdate();

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not create daily island mission.",
                    exception);
        }
    }

    public MissionProgressResult addMissionProgress(
            long islandId,
            LocalDate date,
            int slot,
            int amount) {
        if (amount <= 0) {
            return new MissionProgressResult(
                    false,
                    false,
                    0,
                    0,
                    0);
        }

        Connection connection = databaseManager.getConnection();

        try {
            connection.setAutoCommit(false);

            String selectSql = """
                    SELECT progress,
                           target_amount,
                           xp_reward,
                           completed
                    FROM island_daily_missions
                    WHERE island_id = ?
                      AND mission_date = ?
                      AND slot = ?
                    LIMIT 1;
                    """;

            int oldProgress;
            int target;
            long xpReward;
            boolean completed;

            try (PreparedStatement statement = connection.prepareStatement(
                    selectSql)) {

                statement.setLong(
                        1,
                        islandId);

                statement.setString(
                        2,
                        date.toString());

                statement.setInt(
                        3,
                        slot);

                try (ResultSet results = statement.executeQuery()) {

                    if (!results.next()) {
                        connection.rollback();

                        return new MissionProgressResult(
                                false,
                                false,
                                0,
                                0,
                                0);
                    }

                    oldProgress = results.getInt(
                            "progress");

                    target = results.getInt(
                            "target_amount");

                    xpReward = results.getLong(
                            "xp_reward");

                    completed = results.getInt(
                            "completed") != 0;
                }
            }

            if (completed) {
                connection.rollback();

                return new MissionProgressResult(
                        false,
                        false,
                        oldProgress,
                        target,
                        0);
            }

            int newProgress = Math.min(
                    target,
                    oldProgress + amount);

            boolean completedNow = newProgress >= target;

            String updateSql = """
                    UPDATE island_daily_missions
                    SET progress = ?,
                        completed = ?
                    WHERE island_id = ?
                      AND mission_date = ?
                      AND slot = ?;
                    """;

            try (PreparedStatement statement = connection.prepareStatement(
                    updateSql)) {

                statement.setInt(
                        1,
                        newProgress);

                statement.setInt(
                        2,
                        completedNow ? 1 : 0);

                statement.setLong(
                        3,
                        islandId);

                statement.setString(
                        4,
                        date.toString());

                statement.setInt(
                        5,
                        slot);

                statement.executeUpdate();
            }

            long awardedXp = 0L;

            if (completedNow) {
                ensureProgressRow(
                        connection,
                        islandId);

                String xpSql = """
                        UPDATE island_progression
                        SET mission_xp =
                            mission_xp + ?
                        WHERE island_id = ?;
                        """;

                try (PreparedStatement statement = connection.prepareStatement(
                        xpSql)) {

                    statement.setLong(
                            1,
                            xpReward);

                    statement.setLong(
                            2,
                            islandId);

                    statement.executeUpdate();
                }

                awardedXp = xpReward;
            }

            connection.commit();

            return new MissionProgressResult(
                    newProgress != oldProgress,
                    completedNow,
                    newProgress,
                    target,
                    awardedXp);

        } catch (SQLException exception) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);

            } catch (SQLException ignored) {
            }

            throw new IllegalStateException(
                    "Could not update daily mission progress.",
                    exception);

        } finally {
            try {
                connection.setAutoCommit(true);

            } catch (SQLException ignored) {
            }
        }
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

    private int getBlockCount(
            Connection connection,
            long islandId,
            String blockKey) throws SQLException {

        String sql = """
                SELECT block_count
                FROM island_block_counts
                WHERE island_id = ?
                  AND block_key = ?
                LIMIT 1;
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(
                    1,
                    islandId);

            statement.setString(
                    2,
                    blockKey);

            try (ResultSet results = statement.executeQuery()) {

                if (!results.next()) {
                    return 0;
                }

                return results.getInt(
                        "block_count");
            }
        }
    }

    private void ensureProgressRow(
            long islandId) {
        try {
            ensureProgressRow(
                    databaseManager.getConnection(),
                    islandId);

        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Could not initialize island progression.",
                    exception);
        }
    }

    private void ensureProgressRow(
            Connection connection,
            long islandId) throws SQLException {

        String sql = """
                INSERT OR IGNORE INTO island_progression (
                    island_id,
                    mission_xp,
                    block_score
                )
                VALUES (?, 0, 0);
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setLong(
                    1,
                    islandId);

            statement.executeUpdate();
        }
    }

    private ActiveDailyMission readMission(
            ResultSet results) throws SQLException {

        return new ActiveDailyMission(
                results.getLong(
                        "island_id"),
                LocalDate.parse(
                        results.getString(
                                "mission_date")),
                results.getInt(
                        "slot"),
                results.getString(
                        "mission_id"),
                results.getInt(
                        "target_amount"),
                results.getInt(
                        "progress"),
                results.getLong(
                        "xp_reward"),
                results.getInt(
                        "completed") != 0);
    }

    public record MissionProgressResult(
            boolean changed,
            boolean completedNow,
            int progress,
            int target,
            long awardedXp) {
    }
}