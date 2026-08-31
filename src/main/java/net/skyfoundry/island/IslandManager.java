package net.skyfoundry.island;

import net.skyfoundry.SkyFoundry;
import net.skyfoundry.storage.Database;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class IslandManager {

        private final SkyFoundry plugin;
        private final Database database;
        private final World islandWorld;
        private final IslandPositioner positioner;

        private final Map<Long, Island> islandsById = new HashMap<>();

        private final Map<UUID, Island> islandsByPlayer = new HashMap<>();

        private final Map<UUID, IslandMember> members = new HashMap<>();

        public IslandManager(
                        SkyFoundry plugin,
                        Database database,
                        World islandWorld) {
                this.plugin = plugin;
                this.database = database;
                this.islandWorld = islandWorld;

                int spacing = plugin.getConfig().getInt(
                                "islands.spacing",
                                500);

                this.positioner = new IslandPositioner(
                                spacing);
        }

        public void loadIslands()
                        throws SQLException {

                islandsById.clear();
                islandsByPlayer.clear();
                members.clear();

                loadIslandRecords();
                ensureOwnerMembershipRecords();
                loadMemberRecords();

                plugin.getLogger().info(
                                "Loaded "
                                                + islandsById.size()
                                                + " island(s) and "
                                                + members.size()
                                                + " island member(s).");
        }

        private void loadIslandRecords()
                        throws SQLException {

                String sql = """
                                SELECT
                                    island_id,
                                    owner_uuid,
                                    center_x,
                                    center_z,
                                    home_x,
                                    home_y,
                                    home_z,
                                    home_yaw,
                                    home_pitch,
                                    created_at
                                FROM islands;
                                """;

                try (
                                Statement statement = database
                                                .getConnection()
                                                .createStatement();

                                ResultSet resultSet = statement.executeQuery(sql)) {
                        while (resultSet.next()) {
                                UUID ownerUuid;

                                try {
                                        ownerUuid = UUID.fromString(
                                                        resultSet.getString(
                                                                        "owner_uuid"));

                                } catch (IllegalArgumentException exception) {
                                        plugin.getLogger().warning(
                                                        "Skipping island with invalid owner UUID.");

                                        continue;
                                }

                                Location home = new Location(
                                                islandWorld,
                                                resultSet.getDouble("home_x"),
                                                resultSet.getDouble("home_y"),
                                                resultSet.getDouble("home_z"),
                                                resultSet.getFloat("home_yaw"),
                                                resultSet.getFloat("home_pitch"));

                                Island island = new Island(
                                                resultSet.getLong("island_id"),
                                                ownerUuid,
                                                resultSet.getInt("center_x"),
                                                resultSet.getInt("center_z"),
                                                home,
                                                resultSet.getLong("created_at"));

                                islandsById.put(
                                                island.getIslandId(),
                                                island);
                        }
                }
        }

        private void ensureOwnerMembershipRecords()
                        throws SQLException {

                String checkSql = """
                                SELECT 1
                                FROM island_members
                                WHERE player_uuid = ?;
                                """;

                String insertSql = """
                                INSERT INTO island_members (
                                    island_id,
                                    player_uuid,
                                    role,
                                    home_x,
                                    home_y,
                                    home_z,
                                    home_yaw,
                                    home_pitch,
                                    joined_at
                                )
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                                """;

                for (Island island : islandsById.values()) {

                        UUID ownerUuid = island.getOwnerUuid();

                        boolean exists;

                        try (
                                        PreparedStatement check = database
                                                        .getConnection()
                                                        .prepareStatement(
                                                                        checkSql)) {
                                check.setString(
                                                1,
                                                ownerUuid.toString());

                                try (
                                                ResultSet resultSet = check.executeQuery()) {
                                        exists = resultSet.next();
                                }
                        }

                        if (exists) {
                                continue;
                        }

                        Location home = island.getHome();

                        try (
                                        PreparedStatement insert = database
                                                        .getConnection()
                                                        .prepareStatement(
                                                                        insertSql)) {
                                insert.setLong(
                                                1,
                                                island.getIslandId());

                                insert.setString(
                                                2,
                                                ownerUuid.toString());

                                insert.setString(
                                                3,
                                                IslandRole.OWNER.name());

                                insert.setDouble(
                                                4,
                                                home.getX());

                                insert.setDouble(
                                                5,
                                                home.getY());

                                insert.setDouble(
                                                6,
                                                home.getZ());

                                insert.setFloat(
                                                7,
                                                home.getYaw());

                                insert.setFloat(
                                                8,
                                                home.getPitch());

                                insert.setLong(
                                                9,
                                                island.getCreatedAt());

                                insert.executeUpdate();
                        }
                }
        }

        private void loadMemberRecords()
                        throws SQLException {

                String sql = """
                                SELECT
                                    island_id,
                                    player_uuid,
                                    role,
                                    home_x,
                                    home_y,
                                    home_z,
                                    home_yaw,
                                    home_pitch
                                FROM island_members;
                                """;

                try (
                                Statement statement = database
                                                .getConnection()
                                                .createStatement();

                                ResultSet resultSet = statement.executeQuery(sql)) {
                        while (resultSet.next()) {
                                long islandId = resultSet.getLong(
                                                "island_id");

                                Island island = islandsById.get(
                                                islandId);

                                if (island == null) {
                                        continue;
                                }

                                UUID playerUuid;

                                try {
                                        playerUuid = UUID.fromString(
                                                        resultSet.getString(
                                                                        "player_uuid"));

                                } catch (IllegalArgumentException exception) {
                                        continue;
                                }

                                IslandRole role;

                                try {
                                        role = IslandRole.valueOf(
                                                        resultSet.getString(
                                                                        "role"));

                                } catch (IllegalArgumentException exception) {
                                        plugin.getLogger().warning(
                                                        "Invalid island role for "
                                                                        + playerUuid
                                                                        + ". Defaulting to MEMBER.");

                                        role = IslandRole.MEMBER;
                                }

                                if (playerUuid.equals(
                                                island.getOwnerUuid())) {
                                        role = IslandRole.OWNER;
                                }

                                Location home = new Location(
                                                islandWorld,
                                                resultSet.getDouble("home_x"),
                                                resultSet.getDouble("home_y"),
                                                resultSet.getDouble("home_z"),
                                                resultSet.getFloat("home_yaw"),
                                                resultSet.getFloat("home_pitch"));

                                IslandMember member = new IslandMember(
                                                playerUuid,
                                                role,
                                                home);

                                members.put(
                                                playerUuid,
                                                member);

                                islandsByPlayer.put(
                                                playerUuid,
                                                island);
                        }
                }
        }

        public Island createIsland(
                        UUID ownerUuid) throws SQLException {

                Island existing = islandsByPlayer.get(
                                ownerUuid);

                if (existing != null) {
                        return existing;
                }

                long nextIslandId = getNextIslandId();

                IslandPositioner.IslandPosition position = positioner.getPosition(
                                nextIslandId - 1);

                int creationY = plugin.getConfig().getInt(
                                "islands.creation-y",
                                100);

                Location home = createDefaultHome(
                                position.x(),
                                position.z(),
                                creationY);

                long createdAt = Instant.now().getEpochSecond();

                Island island = new Island(
                                nextIslandId,
                                ownerUuid,
                                position.x(),
                                position.z(),
                                home,
                                createdAt);

                insertIsland(
                                island);

                try {
                        insertMember(
                                        island,
                                        ownerUuid,
                                        IslandRole.OWNER,
                                        home);

                } catch (SQLException exception) {
                        deleteIslandRecord(
                                        ownerUuid);

                        throw exception;
                }

                IslandMember owner = new IslandMember(
                                ownerUuid,
                                IslandRole.OWNER,
                                home);

                islandsById.put(
                                island.getIslandId(),
                                island);

                islandsByPlayer.put(
                                ownerUuid,
                                island);

                members.put(
                                ownerUuid,
                                owner);

                return island;
        }

        public boolean setHome(
                        UUID playerUuid,
                        Location location) throws SQLException {

                Island island = islandsByPlayer.get(
                                playerUuid);

                IslandMember member = members.get(
                                playerUuid);

                if (island == null
                                || member == null) {
                        return false;
                }

                int size = getIslandSize();

                if (!island.contains(
                                location,
                                size)) {
                        return false;
                }

                String sql = """
                                UPDATE island_members
                                SET
                                    home_x = ?,
                                    home_y = ?,
                                    home_z = ?,
                                    home_yaw = ?,
                                    home_pitch = ?
                                WHERE player_uuid = ?;
                                """;

                try (
                                PreparedStatement statement = database
                                                .getConnection()
                                                .prepareStatement(sql)) {
                        statement.setDouble(
                                        1,
                                        location.getX());

                        statement.setDouble(
                                        2,
                                        location.getY());

                        statement.setDouble(
                                        3,
                                        location.getZ());

                        statement.setFloat(
                                        4,
                                        location.getYaw());

                        statement.setFloat(
                                        5,
                                        location.getPitch());

                        statement.setString(
                                        6,
                                        playerUuid.toString());

                        statement.executeUpdate();
                }

                member.setHome(
                                location);

                if (member.getRole() == IslandRole.OWNER) {

                        island.setHome(
                                        location);

                        updateLegacyOwnerHome(
                                        island);
                }

                return true;
        }

        public boolean invite(
                        UUID inviterUuid,
                        UUID targetUuid) throws SQLException {

                Island island = islandsByPlayer.get(
                                inviterUuid);

                IslandMember inviter = members.get(
                                inviterUuid);

                if (island == null
                                || inviter == null
                                || !inviter.getRole().canInvite()) {

                        return false;
                }

                if (islandsByPlayer.containsKey(
                                targetUuid)) {
                        return false;
                }

                int memberLimit = Math.max(
                                1,
                                plugin.getConfig().getInt(
                                                "islands.member-limit",
                                                5));

                if (getMemberCount(island) >= memberLimit) {

                        return false;
                }

                String sql = """
                                INSERT INTO island_invites (
                                    island_id,
                                    player_uuid,
                                    inviter_uuid,
                                    created_at
                                )
                                VALUES (?, ?, ?, ?)
                                ON CONFLICT (
                                    island_id,
                                    player_uuid
                                )
                                DO UPDATE SET
                                    inviter_uuid = excluded.inviter_uuid,
                                    created_at = excluded.created_at;
                                """;

                try (
                                PreparedStatement statement = database
                                                .getConnection()
                                                .prepareStatement(sql)) {
                        statement.setLong(
                                        1,
                                        island.getIslandId());

                        statement.setString(
                                        2,
                                        targetUuid.toString());

                        statement.setString(
                                        3,
                                        inviterUuid.toString());

                        statement.setLong(
                                        4,
                                        Instant.now()
                                                        .getEpochSecond());

                        statement.executeUpdate();
                }

                return true;
        }

        public Optional<Island> acceptLatestInvite(
                        UUID playerUuid) throws SQLException {

                if (islandsByPlayer.containsKey(
                                playerUuid)) {
                        return Optional.empty();
                }

                long cutoff = Instant.now().getEpochSecond()
                                - getInviteTimeoutSeconds();

                String lookupSql = """
                                SELECT
                                    island_id
                                FROM island_invites
                                WHERE
                                    player_uuid = ?
                                    AND created_at >= ?
                                ORDER BY created_at DESC
                                LIMIT 1;
                                """;

                long islandId;

                try (
                                PreparedStatement statement = database
                                                .getConnection()
                                                .prepareStatement(
                                                                lookupSql)) {
                        statement.setString(
                                        1,
                                        playerUuid.toString());

                        statement.setLong(
                                        2,
                                        cutoff);

                        try (
                                        ResultSet resultSet = statement.executeQuery()) {
                                if (!resultSet.next()) {
                                        deleteExpiredInvites(
                                                        playerUuid,
                                                        cutoff);

                                        return Optional.empty();
                                }

                                islandId = resultSet.getLong(
                                                "island_id");
                        }
                }

                Island island = islandsById.get(
                                islandId);

                if (island == null) {
                        return Optional.empty();
                }

                int memberLimit = Math.max(
                                1,
                                plugin.getConfig().getInt(
                                                "islands.member-limit",
                                                5));

                if (getMemberCount(island) >= memberLimit) {

                        return Optional.empty();
                }

                Location home = island.getHome();

                insertMember(
                                island,
                                playerUuid,
                                IslandRole.MEMBER,
                                home);

                deleteInvitesForPlayer(
                                playerUuid);

                IslandMember member = new IslandMember(
                                playerUuid,
                                IslandRole.MEMBER,
                                home);

                members.put(
                                playerUuid,
                                member);

                islandsByPlayer.put(
                                playerUuid,
                                island);

                return Optional.of(
                                island);
        }

        public boolean leaveIsland(
                        UUID playerUuid) throws SQLException {

                Island island = islandsByPlayer.get(
                                playerUuid);

                IslandMember member = members.get(
                                playerUuid);

                if (island == null
                                || member == null
                                || !member.getRole()
                                                .canLeaveIsland()) {

                        return false;
                }

                String sql = """
                                DELETE FROM island_members
                                WHERE player_uuid = ?;
                                """;

                try (
                                PreparedStatement statement = database
                                                .getConnection()
                                                .prepareStatement(sql)) {
                        statement.setString(
                                        1,
                                        playerUuid.toString());

                        statement.executeUpdate();
                }

                islandsByPlayer.remove(
                                playerUuid);

                members.remove(
                                playerUuid);

                return true;
        }

        public boolean setRole(
                        UUID actorUuid,
                        UUID targetUuid,
                        IslandRole newRole) throws SQLException {

                Island actorIsland = islandsByPlayer.get(
                                actorUuid);

                Island targetIsland = islandsByPlayer.get(
                                targetUuid);

                IslandMember actor = members.get(
                                actorUuid);

                IslandMember target = members.get(
                                targetUuid);

                if (actorIsland == null
                                || targetIsland == null
                                || actor == null
                                || target == null) {

                        return false;
                }

                if (actorIsland.getIslandId() != targetIsland.getIslandId()) {

                        return false;
                }

                if (!actor.getRole()
                                .canChangeRoles()) {

                        return false;
                }

                if (target.getRole() == IslandRole.OWNER) {

                        return false;
                }

                if (newRole == IslandRole.OWNER) {
                        return false;
                }

                String sql = """
                                UPDATE island_members
                                SET role = ?
                                WHERE player_uuid = ?;
                                """;

                try (
                                PreparedStatement statement = database
                                                .getConnection()
                                                .prepareStatement(sql)) {
                        statement.setString(
                                        1,
                                        newRole.name());

                        statement.setString(
                                        2,
                                        targetUuid.toString());

                        statement.executeUpdate();
                }

                target.setRole(
                                newRole);

                return true;
        }

        public CompletableFuture<Boolean> deleteIsland(
                        UUID ownerUuid) {
                Island island = islandsByPlayer.get(
                                ownerUuid);

                IslandMember member = members.get(
                                ownerUuid);

                if (island == null
                                || member == null
                                || member.getRole() != IslandRole.OWNER
                                || !island.getOwnerUuid()
                                                .equals(ownerUuid)) {

                        return CompletableFuture.completedFuture(
                                        false);
                }

                teleportPlayersOffIsland(
                                island);

                return clearIsland(
                                island).thenApply(unused -> {
                                        try {
                                                deleteIslandRecord(
                                                                ownerUuid);

                                                removeIslandFromMemory(
                                                                island);

                                                return true;

                                        } catch (SQLException exception) {
                                                throw new RuntimeException(
                                                                "Failed to delete island database record.",
                                                                exception);
                                        }
                                });
        }

        private void removeIslandFromMemory(
                        Island island) {
                islandsById.remove(
                                island.getIslandId());

                islandsByPlayer
                                .entrySet()
                                .removeIf(
                                                entry -> entry.getValue()
                                                                .getIslandId() == island.getIslandId());

                members
                                .keySet()
                                .removeIf(
                                                uuid -> !islandsByPlayer.containsKey(
                                                                uuid));
        }

        public Optional<Island> getIsland(
                        UUID playerUuid) {
                return Optional.ofNullable(
                                islandsByPlayer.get(
                                                playerUuid));
        }

        public Optional<Island> getOwnedIsland(
                        UUID ownerUuid) {
                Island island = islandsByPlayer.get(
                                ownerUuid);

                if (island == null
                                || !island.getOwnerUuid()
                                                .equals(ownerUuid)) {

                        return Optional.empty();
                }

                return Optional.of(
                                island);
        }

        public Optional<IslandMember> getMember(
                        UUID playerUuid) {
                return Optional.ofNullable(
                                members.get(
                                                playerUuid));
        }

        public Optional<IslandRole> getRole(
                        UUID playerUuid) {
                IslandMember member = members.get(
                                playerUuid);

                if (member == null) {
                        return Optional.empty();
                }

                return Optional.of(
                                member.getRole());
        }

        public Location getHome(
                        UUID playerUuid) {
                IslandMember member = members.get(
                                playerUuid);

                if (member == null) {
                        return null;
                }

                return member.getHome();
        }

        public boolean hasIsland(
                        UUID playerUuid) {
                return islandsByPlayer.containsKey(
                                playerUuid);
        }

        public boolean isMemberOf(
                        UUID playerUuid,
                        Island island) {
                Island playerIsland = islandsByPlayer.get(
                                playerUuid);

                return playerIsland != null
                                && playerIsland.getIslandId() == island.getIslandId();
        }

        public Optional<Island> getIslandAt(
                        Location location) {
                if (location.getWorld() == null
                                || !location.getWorld()
                                                .equals(islandWorld)) {

                        return Optional.empty();
                }

                int size = getIslandSize();

                for (Island island : islandsById.values()) {

                        if (island.contains(
                                        location,
                                        size)) {
                                return Optional.of(
                                                island);
                        }
                }

                return Optional.empty();
        }

        public int getMemberCount(
                        Island island) {
                int count = 0;

                for (Island memberIsland : islandsByPlayer.values()) {

                        if (memberIsland.getIslandId() == island.getIslandId()) {

                                count++;
                        }
                }

                return count;
        }

        public Collection<Island> getIslands() {
                return Collections.unmodifiableCollection(
                                islandsById.values());
        }

        public int getIslandCount() {
                return islandsById.size();
        }

        private void insertMember(
                        Island island,
                        UUID playerUuid,
                        IslandRole role,
                        Location home) throws SQLException {

                String sql = """
                                INSERT INTO island_members (
                                    island_id,
                                    player_uuid,
                                    role,
                                    home_x,
                                    home_y,
                                    home_z,
                                    home_yaw,
                                    home_pitch,
                                    joined_at
                                )
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                                """;

                try (
                                PreparedStatement statement = database
                                                .getConnection()
                                                .prepareStatement(sql)) {
                        statement.setLong(
                                        1,
                                        island.getIslandId());

                        statement.setString(
                                        2,
                                        playerUuid.toString());

                        statement.setString(
                                        3,
                                        role.name());

                        statement.setDouble(
                                        4,
                                        home.getX());

                        statement.setDouble(
                                        5,
                                        home.getY());

                        statement.setDouble(
                                        6,
                                        home.getZ());

                        statement.setFloat(
                                        7,
                                        home.getYaw());

                        statement.setFloat(
                                        8,
                                        home.getPitch());

                        statement.setLong(
                                        9,
                                        Instant.now()
                                                        .getEpochSecond());

                        statement.executeUpdate();
                }
        }

        private void updateLegacyOwnerHome(
                        Island island) throws SQLException {

                Location home = island.getHome();

                String sql = """
                                UPDATE islands
                                SET
                                    home_x = ?,
                                    home_y = ?,
                                    home_z = ?,
                                    home_yaw = ?,
                                    home_pitch = ?
                                WHERE island_id = ?;
                                """;

                try (
                                PreparedStatement statement = database
                                                .getConnection()
                                                .prepareStatement(sql)) {
                        statement.setDouble(
                                        1,
                                        home.getX());

                        statement.setDouble(
                                        2,
                                        home.getY());

                        statement.setDouble(
                                        3,
                                        home.getZ());

                        statement.setFloat(
                                        4,
                                        home.getYaw());

                        statement.setFloat(
                                        5,
                                        home.getPitch());

                        statement.setLong(
                                        6,
                                        island.getIslandId());

                        statement.executeUpdate();
                }
        }

        private void insertIsland(
                        Island island) throws SQLException {

                String sql = """
                                INSERT INTO islands (
                                    island_id,
                                    owner_uuid,
                                    center_x,
                                    center_z,
                                    home_x,
                                    home_y,
                                    home_z,
                                    home_yaw,
                                    home_pitch,
                                    created_at
                                )
                                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                                """;

                Location home = island.getHome();

                try (
                                PreparedStatement statement = database
                                                .getConnection()
                                                .prepareStatement(sql)) {
                        statement.setLong(
                                        1,
                                        island.getIslandId());

                        statement.setString(
                                        2,
                                        island.getOwnerUuid()
                                                        .toString());

                        statement.setInt(
                                        3,
                                        island.getCenterX());

                        statement.setInt(
                                        4,
                                        island.getCenterZ());

                        statement.setDouble(
                                        5,
                                        home.getX());

                        statement.setDouble(
                                        6,
                                        home.getY());

                        statement.setDouble(
                                        7,
                                        home.getZ());

                        statement.setFloat(
                                        8,
                                        home.getYaw());

                        statement.setFloat(
                                        9,
                                        home.getPitch());

                        statement.setLong(
                                        10,
                                        island.getCreatedAt());

                        statement.executeUpdate();
                }
        }

        private void deleteIslandRecord(
                        UUID ownerUuid) throws SQLException {

                String sql = """
                                DELETE FROM islands
                                WHERE owner_uuid = ?;
                                """;

                try (
                                PreparedStatement statement = database
                                                .getConnection()
                                                .prepareStatement(sql)) {
                        statement.setString(
                                        1,
                                        ownerUuid.toString());

                        statement.executeUpdate();
                }
        }

        private void deleteInvitesForPlayer(
                        UUID playerUuid) throws SQLException {

                String sql = """
                                DELETE FROM island_invites
                                WHERE player_uuid = ?;
                                """;

                try (
                                PreparedStatement statement = database
                                                .getConnection()
                                                .prepareStatement(sql)) {
                        statement.setString(
                                        1,
                                        playerUuid.toString());

                        statement.executeUpdate();
                }
        }

        private void deleteExpiredInvites(
                        UUID playerUuid,
                        long cutoff) throws SQLException {

                String sql = """
                                DELETE FROM island_invites
                                WHERE
                                    player_uuid = ?
                                    AND created_at < ?;
                                """;

                try (
                                PreparedStatement statement = database
                                                .getConnection()
                                                .prepareStatement(sql)) {
                        statement.setString(
                                        1,
                                        playerUuid.toString());

                        statement.setLong(
                                        2,
                                        cutoff);

                        statement.executeUpdate();
                }
        }

        private long getInviteTimeoutSeconds() {
                return Math.max(
                                1,
                                plugin.getConfig().getLong(
                                                "invites.timeout-seconds",
                                                120));
        }

        private long getNextIslandId()
                        throws SQLException {

                String sql = """
                                SELECT seq
                                FROM sqlite_sequence
                                WHERE name = 'islands';
                                """;

                try (
                                Statement statement = database
                                                .getConnection()
                                                .createStatement();

                                ResultSet resultSet = statement.executeQuery(sql)) {
                        if (resultSet.next()) {
                                return resultSet.getLong(
                                                "seq") + 1;
                        }
                }

                return 1;
        }

        private Location createDefaultHome(
                        int centerX,
                        int centerZ,
                        int creationY) {
                double offsetX = plugin.getConfig().getDouble(
                                "islands.spawn-offset.x",
                                0.5);

                double offsetY = plugin.getConfig().getDouble(
                                "islands.spawn-offset.y",
                                8.0);

                double offsetZ = plugin.getConfig().getDouble(
                                "islands.spawn-offset.z",
                                0.5);

                float yaw = (float) plugin.getConfig()
                                .getDouble(
                                                "islands.spawn-offset.yaw",
                                                0.0);

                float pitch = (float) plugin.getConfig()
                                .getDouble(
                                                "islands.spawn-offset.pitch",
                                                0.0);

                return new Location(
                                islandWorld,
                                centerX + offsetX,
                                creationY + offsetY,
                                centerZ + offsetZ,
                                yaw,
                                pitch);
        }

        private int getIslandSize() {
                return Math.max(
                                1,
                                plugin.getConfig().getInt(
                                                "islands.size",
                                                50));
        }

        private CompletableFuture<Void> clearIsland(
                        Island island) {
                CompletableFuture<Void> future = new CompletableFuture<>();

                int islandSize = getIslandSize();

                int positionsPerTick = Math.max(
                                1,
                                plugin.getConfig().getInt(
                                                "deletion.positions-per-tick",
                                                25000));

                int minX = island.getCenterX()
                                - (islandSize / 2);

                int minZ = island.getCenterZ()
                                - (islandSize / 2);

                int maxX = minX + islandSize - 1;

                int maxZ = minZ + islandSize - 1;

                int minY = islandWorld.getMinHeight();

                int maxY = islandWorld.getMaxHeight() - 1;

                removeIslandEntities(
                                minX,
                                maxX,
                                minY,
                                maxY,
                                minZ,
                                maxZ);

                final int[] x = { minX };
                final int[] y = { minY };
                final int[] z = { minZ };

                final BukkitTask[] task = new BukkitTask[1];

                task[0] = plugin.getServer()
                                .getScheduler()
                                .runTaskTimer(
                                                plugin,
                                                () -> {
                                                        try {
                                                                int processed = 0;

                                                                while (processed < positionsPerTick) {

                                                                        if (x[0] > maxX) {
                                                                                task[0].cancel();
                                                                                future.complete(null);
                                                                                return;
                                                                        }

                                                                        var block = islandWorld.getBlockAt(
                                                                                        x[0],
                                                                                        y[0],
                                                                                        z[0]);

                                                                        if (block.getType() != Material.AIR) {

                                                                                block.setType(
                                                                                                Material.AIR,
                                                                                                false);
                                                                        }

                                                                        processed++;
                                                                        y[0]++;

                                                                        if (y[0] > maxY) {
                                                                                y[0] = minY;
                                                                                z[0]++;

                                                                                if (z[0] > maxZ) {
                                                                                        z[0] = minZ;
                                                                                        x[0]++;
                                                                                }
                                                                        }
                                                                }

                                                        } catch (Exception exception) {
                                                                task[0].cancel();

                                                                future.completeExceptionally(
                                                                                exception);
                                                        }
                                                },
                                                1L,
                                                1L);

                return future;
        }

        private void teleportPlayersOffIsland(
                        Island island) {
                Location destination = getSafeTeleportLocation();

                int islandSize = getIslandSize();

                for (Player player : islandWorld.getPlayers()) {

                        if (island.contains(
                                        player.getLocation(),
                                        islandSize)) {
                                player.teleport(
                                                destination);
                        }
                }
        }

        public Location getSafeTeleportLocation() {
                String configuredWorld = plugin.getConfig().getString(
                                "deletion.teleport-world",
                                "world");

                if (configuredWorld != null
                                && !configuredWorld.isBlank()) {

                        World world = plugin.getServer().getWorld(
                                        configuredWorld);

                        if (world != null
                                        && !world.equals(
                                                        islandWorld)) {
                                return world.getSpawnLocation()
                                                .clone()
                                                .add(
                                                                0.5,
                                                                0.0,
                                                                0.5);
                        }
                }

                for (World world : plugin.getServer().getWorlds()) {

                        if (!world.equals(
                                        islandWorld)) {
                                return world.getSpawnLocation()
                                                .clone()
                                                .add(
                                                                0.5,
                                                                0.0,
                                                                0.5);
                        }
                }

                return islandWorld
                                .getSpawnLocation()
                                .clone()
                                .add(
                                                0.5,
                                                0.0,
                                                0.5);
        }

        private void removeIslandEntities(
                        int minX,
                        int maxX,
                        int minY,
                        int maxY,
                        int minZ,
                        int maxZ) {
                for (Entity entity : islandWorld.getEntities()) {

                        if (entity instanceof Player) {
                                continue;
                        }

                        Location location = entity.getLocation();

                        if (location.getX() >= minX
                                        && location.getX() < maxX + 1.0
                                        && location.getY() >= minY
                                        && location.getY() < maxY + 1.0
                                        && location.getZ() >= minZ
                                        && location.getZ() < maxZ + 1.0) {

                                entity.remove();
                        }
                }
        }

        public boolean transferOwnership(
                        UUID currentOwnerUuid,
                        UUID newOwnerUuid) throws SQLException {

                if (currentOwnerUuid.equals(
                                newOwnerUuid)) {
                        return false;
                }

                Island island = islandsByPlayer.get(
                                currentOwnerUuid);

                Island newOwnerIsland = islandsByPlayer.get(
                                newOwnerUuid);

                IslandMember currentOwner = members.get(
                                currentOwnerUuid);

                IslandMember newOwner = members.get(
                                newOwnerUuid);

                if (island == null
                                || newOwnerIsland == null
                                || currentOwner == null
                                || newOwner == null) {

                        return false;
                }

                if (island.getIslandId() != newOwnerIsland.getIslandId()) {

                        return false;
                }

                if (!island.getOwnerUuid()
                                .equals(
                                                currentOwnerUuid)) {

                        return false;
                }

                if (!currentOwner
                                .getRole()
                                .canTransferOwnership()) {

                        return false;
                }

                var connection = database.getConnection();

                boolean previousAutoCommit = connection.getAutoCommit();

                try {
                        connection.setAutoCommit(
                                        false);

                        String updateIslandSql = """
                                        UPDATE islands
                                        SET
                                            owner_uuid = ?,
                                            home_x = ?,
                                            home_y = ?,
                                            home_z = ?,
                                            home_yaw = ?,
                                            home_pitch = ?
                                        WHERE
                                            island_id = ?
                                            AND owner_uuid = ?;
                                        """;

                        Location newOwnerHome = newOwner.getHome();

                        try (
                                        PreparedStatement statement = connection.prepareStatement(
                                                        updateIslandSql)) {
                                statement.setString(
                                                1,
                                                newOwnerUuid.toString());

                                statement.setDouble(
                                                2,
                                                newOwnerHome.getX());

                                statement.setDouble(
                                                3,
                                                newOwnerHome.getY());

                                statement.setDouble(
                                                4,
                                                newOwnerHome.getZ());

                                statement.setFloat(
                                                5,
                                                newOwnerHome.getYaw());

                                statement.setFloat(
                                                6,
                                                newOwnerHome.getPitch());

                                statement.setLong(
                                                7,
                                                island.getIslandId());

                                statement.setString(
                                                8,
                                                currentOwnerUuid.toString());

                                int changed = statement.executeUpdate();

                                if (changed != 1) {
                                        connection.rollback();

                                        return false;
                                }
                        }

                        String updateRoleSql = """
                                        UPDATE island_members
                                        SET role = ?
                                        WHERE player_uuid = ?;
                                        """;

                        try (
                                        PreparedStatement statement = connection.prepareStatement(
                                                        updateRoleSql)) {
                                statement.setString(
                                                1,
                                                IslandRole.CO_OWNER.name());

                                statement.setString(
                                                2,
                                                currentOwnerUuid.toString());

                                statement.executeUpdate();

                                statement.setString(
                                                1,
                                                IslandRole.OWNER.name());

                                statement.setString(
                                                2,
                                                newOwnerUuid.toString());

                                statement.executeUpdate();
                        }

                        connection.commit();

                        island.setOwnerUuid(
                                        newOwnerUuid);

                        island.setHome(
                                        newOwnerHome);

                        currentOwner.setRole(
                                        IslandRole.CO_OWNER);

                        newOwner.setRole(
                                        IslandRole.OWNER);

                        return true;

                } catch (SQLException exception) {
                        connection.rollback();

                        throw exception;

                } finally {
                        connection.setAutoCommit(
                                        previousAutoCommit);
                }
        }

        public boolean kickMember(
                        UUID actorUuid,
                        UUID targetUuid) throws SQLException {

                if (actorUuid.equals(
                                targetUuid)) {
                        return false;
                }

                Island actorIsland = islandsByPlayer.get(
                                actorUuid);

                Island targetIsland = islandsByPlayer.get(
                                targetUuid);

                IslandMember actor = members.get(
                                actorUuid);

                IslandMember target = members.get(
                                targetUuid);

                if (actorIsland == null
                                || targetIsland == null
                                || actor == null
                                || target == null) {

                        return false;
                }

                if (actorIsland.getIslandId() != targetIsland.getIslandId()) {

                        return false;
                }

                if (!actor.getRole()
                                .canKick()) {

                        return false;
                }

                if (target.getRole() == IslandRole.OWNER) {

                        return false;
                }

                if (actor.getRole() == IslandRole.CO_OWNER
                                && target.getRole() != IslandRole.MEMBER) {

                        return false;
                }

                String sql = """
                                DELETE FROM island_members
                                WHERE
                                    island_id = ?
                                    AND player_uuid = ?;
                                """;

                try (
                                PreparedStatement statement = database
                                                .getConnection()
                                                .prepareStatement(
                                                                sql)) {
                        statement.setLong(
                                        1,
                                        actorIsland.getIslandId());

                        statement.setString(
                                        2,
                                        targetUuid.toString());

                        int changed = statement.executeUpdate();

                        if (changed != 1) {
                                return false;
                        }
                }

                islandsByPlayer.remove(
                                targetUuid);

                members.remove(
                                targetUuid);

                Player targetPlayer = plugin.getServer()
                                .getPlayer(
                                                targetUuid);

                if (targetPlayer != null
                                && targetPlayer.isOnline()) {

                        if (actorIsland.contains(
                                        targetPlayer.getLocation(),
                                        getIslandSize())) {
                                targetPlayer.teleport(
                                                getSafeTeleportLocation());
                        }
                }

                return true;
        }
}