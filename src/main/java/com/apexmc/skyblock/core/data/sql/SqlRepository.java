package com.apexmc.skyblock.core.data.sql;

import com.apexmc.skyblock.core.data.BlockPosition;
import com.apexmc.skyblock.core.data.GeneratorState;
import com.apexmc.skyblock.core.data.IslandProfile;
import com.apexmc.skyblock.core.data.PlayerProfile;
import com.apexmc.skyblock.core.data.ProfileCodec;
import com.apexmc.skyblock.core.data.ProgressionRepository;
import com.apexmc.skyblock.core.data.SpawnerState;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC repository supporting SQLite (default) and MySQL/MariaDB. All methods are
 * blocking and must only be called from the plugin's asynchronous executor.
 */
public final class SqlRepository implements ProgressionRepository {

    private static final List<String> PLAYER_COLUMNS = List.of(
            "player_id", "island_id", "season_id", "active_role", "tokens",
            "role_xp", "stats", "tool_upgrades", "companions", "active_companion");
    private static final List<String> ISLAND_COLUMNS = List.of(
            "island_id", "owner_id", "season_id", "island_xp", "mastery_xp", "unlocks");

    private final SqlSettings settings;
    private Connection connection;

    public SqlRepository(SqlSettings settings) {
        this.settings = settings;
    }

    private synchronized Connection connection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName(settings.dialect().driverClass());
            } catch (ClassNotFoundException ignored) {
                // Paper ships the SQLite driver; MySQL drivers are provided by the server.
            }
            connection = settings.username().isBlank()
                    ? DriverManager.getConnection(settings.jdbcUrl())
                    : DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password());
        }
        return connection;
    }

    private String table(String name) {
        return settings.tablePrefix() + name;
    }

    @Override
    public void initialize() {
        try (Statement statement = connection().createStatement()) {
            for (String ddl : settings.dialect().schema(settings.tablePrefix())) {
                statement.executeUpdate(ddl);
            }
        } catch (SQLException ex) {
            throw new StorageException("failed to initialise the progression database", ex);
        }
    }

    @Override
    public Optional<PlayerProfile> loadPlayer(UUID playerId) {
        String sql = "SELECT * FROM " + table("players") + " WHERE player_id = ?";
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, playerId.toString());
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return Optional.empty();
                }
                Map<String, String> row = new LinkedHashMap<>();
                for (String column : PLAYER_COLUMNS) {
                    String value = results.getString(column);
                    row.put(column, value == null ? "" : value);
                }
                return Optional.of(ProfileCodec.decodePlayer(row));
            }
        } catch (SQLException ex) {
            throw new StorageException("failed to load player " + playerId, ex);
        }
    }

    @Override
    public void savePlayer(PlayerProfile profile) {
        Map<String, String> row = ProfileCodec.encodePlayer(profile);
        String sql = settings.dialect().upsert(table("players"), "player_id", PLAYER_COLUMNS);
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            int index = 1;
            for (String column : PLAYER_COLUMNS) {
                if (column.equals("tokens")) {
                    statement.setLong(index++, profile.tokens());
                } else {
                    statement.setString(index++, row.getOrDefault(column, ""));
                }
            }
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("failed to save player " + profile.playerId(), ex);
        }
    }

    @Override
    public Optional<IslandProfile> loadIsland(UUID islandId) {
        return loadIslandWhere("island_id", islandId.toString());
    }

    @Override
    public Optional<IslandProfile> loadIslandByOwner(UUID ownerId) {
        return loadIslandWhere("owner_id", ownerId.toString());
    }

    private Optional<IslandProfile> loadIslandWhere(String column, String value) {
        String sql = "SELECT * FROM " + table("islands") + " WHERE " + column + " = ? LIMIT 1";
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, value);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return Optional.empty();
                }
                Map<String, String> row = new LinkedHashMap<>();
                for (String name : ISLAND_COLUMNS) {
                    String stored = results.getString(name);
                    row.put(name, stored == null ? "" : stored);
                }
                return Optional.of(ProfileCodec.decodeIsland(row));
            }
        } catch (SQLException ex) {
            throw new StorageException("failed to load island by " + column, ex);
        }
    }

    @Override
    public void saveIsland(IslandProfile island) {
        Map<String, String> row = ProfileCodec.encodeIsland(island);
        String sql = settings.dialect().upsert(table("islands"), "island_id", ISLAND_COLUMNS);
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            int index = 1;
            for (String column : ISLAND_COLUMNS) {
                if (column.equals("island_xp")) {
                    statement.setLong(index++, island.islandXp());
                } else {
                    statement.setString(index++, row.getOrDefault(column, ""));
                }
            }
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("failed to save island " + island.islandId(), ex);
        }
    }

    @Override
    public List<GeneratorState> loadGenerators() {
        List<GeneratorState> states = new ArrayList<>();
        String sql = "SELECT * FROM " + table("generators");
        try (PreparedStatement statement = connection().prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                states.add(new GeneratorState(
                        UUID.fromString(results.getString("generator_id")),
                        UUID.fromString(results.getString("island_id")),
                        new BlockPosition(results.getString("world"), results.getInt("x"), results.getInt("y"), results.getInt("z")),
                        results.getString("type_id"),
                        results.getInt("tier"),
                        results.getLong("last_produce")));
            }
        } catch (SQLException ex) {
            throw new StorageException("failed to load generators", ex);
        }
        return states;
    }

    @Override
    public void saveGenerators(Collection<GeneratorState> generators) {
        if (generators.isEmpty()) {
            return;
        }
        List<String> columns = List.of("generator_id", "island_id", "world", "x", "y", "z", "type_id", "tier", "last_produce");
        String sql = settings.dialect().upsert(table("generators"), "generator_id", columns);
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            for (GeneratorState state : generators) {
                statement.setString(1, state.id().toString());
                statement.setString(2, state.islandId().toString());
                statement.setString(3, state.position().world());
                statement.setInt(4, state.position().x());
                statement.setInt(5, state.position().y());
                statement.setInt(6, state.position().z());
                statement.setString(7, state.typeId());
                statement.setInt(8, state.tier());
                statement.setLong(9, state.lastProduceMillis());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            throw new StorageException("failed to save generators", ex);
        }
    }

    @Override
    public void deleteGenerator(UUID generatorId) {
        delete(table("generators"), "generator_id", generatorId);
    }

    @Override
    public List<SpawnerState> loadSpawners() {
        List<SpawnerState> states = new ArrayList<>();
        String sql = "SELECT * FROM " + table("spawners");
        try (PreparedStatement statement = connection().prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                states.add(new SpawnerState(
                        UUID.fromString(results.getString("spawner_id")),
                        UUID.fromString(results.getString("island_id")),
                        new BlockPosition(results.getString("world"), results.getInt("x"), results.getInt("y"), results.getInt("z")),
                        results.getString("type_id"),
                        results.getInt("tier"),
                        results.getInt("stack_size"),
                        results.getLong("last_spawn")));
            }
        } catch (SQLException ex) {
            throw new StorageException("failed to load spawners", ex);
        }
        return states;
    }

    @Override
    public void saveSpawners(Collection<SpawnerState> spawners) {
        if (spawners.isEmpty()) {
            return;
        }
        List<String> columns = List.of("spawner_id", "island_id", "world", "x", "y", "z", "type_id", "tier", "stack_size", "last_spawn");
        String sql = settings.dialect().upsert(table("spawners"), "spawner_id", columns);
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            for (SpawnerState state : spawners) {
                statement.setString(1, state.id().toString());
                statement.setString(2, state.islandId().toString());
                statement.setString(3, state.position().world());
                statement.setInt(4, state.position().x());
                statement.setInt(5, state.position().y());
                statement.setInt(6, state.position().z());
                statement.setString(7, state.typeId());
                statement.setInt(8, state.tier());
                statement.setInt(9, state.stackSize());
                statement.setLong(10, state.lastSpawnMillis());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            throw new StorageException("failed to save spawners", ex);
        }
    }

    @Override
    public void deleteSpawner(UUID spawnerId) {
        delete(table("spawners"), "spawner_id", spawnerId);
    }

    private void delete(String table, String column, UUID id) {
        try (PreparedStatement statement = connection().prepareStatement("DELETE FROM " + table + " WHERE " + column + " = ?")) {
            statement.setString(1, id.toString());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("failed to delete from " + table, ex);
        }
    }

    @Override
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // Closing on shutdown: nothing useful can be done with the error.
            }
            connection = null;
        }
    }
}
