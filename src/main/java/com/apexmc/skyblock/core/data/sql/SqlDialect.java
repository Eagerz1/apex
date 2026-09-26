package com.apexmc.skyblock.core.data.sql;

import java.util.List;
import java.util.Locale;

/** SQL differences between SQLite (default) and MySQL/MariaDB. */
public enum SqlDialect {

    SQLITE,
    MYSQL;

    public static SqlDialect fromId(String id) {
        String normalised = id == null ? "sqlite" : id.trim().toLowerCase(Locale.ROOT);
        return switch (normalised) {
            case "mysql", "mariadb" -> MYSQL;
            case "sqlite", "" -> SQLITE;
            default -> throw new IllegalArgumentException("unsupported storage type: " + id);
        };
    }

    /** Text column type; MySQL needs an explicit length for indexed keys. */
    private String text() {
        return this == MYSQL ? "TEXT" : "TEXT";
    }

    private String key() {
        return this == MYSQL ? "VARCHAR(36)" : "TEXT";
    }

    /** DDL for every table, in creation order. */
    public List<String> schema(String prefix) {
        return List.of(
                "CREATE TABLE IF NOT EXISTS " + prefix + "players ("
                        + "player_id " + key() + " PRIMARY KEY,"
                        + "island_id " + key() + ","
                        + "season_id " + key() + ","
                        + "active_role " + key() + ","
                        + "tokens BIGINT NOT NULL DEFAULT 0,"
                        + "role_xp " + text() + ","
                        + "stats " + text() + ","
                        + "tool_upgrades " + text() + ","
                        + "companions " + text() + ","
                        + "active_companion " + key() + ")",
                "CREATE TABLE IF NOT EXISTS " + prefix + "islands ("
                        + "island_id " + key() + " PRIMARY KEY,"
                        + "owner_id " + key() + ","
                        + "season_id " + key() + ","
                        + "island_xp BIGINT NOT NULL DEFAULT 0,"
                        + "mastery_xp " + text() + ","
                        + "unlocks " + text() + ")",
                "CREATE TABLE IF NOT EXISTS " + prefix + "generators ("
                        + "generator_id " + key() + " PRIMARY KEY,"
                        + "island_id " + key() + ","
                        + "world " + key() + ","
                        + "x INT NOT NULL, y INT NOT NULL, z INT NOT NULL,"
                        + "type_id " + key() + ","
                        + "tier INT NOT NULL DEFAULT 1,"
                        + "last_produce BIGINT NOT NULL DEFAULT 0)",
                "CREATE TABLE IF NOT EXISTS " + prefix + "spawners ("
                        + "spawner_id " + key() + " PRIMARY KEY,"
                        + "island_id " + key() + ","
                        + "world " + key() + ","
                        + "x INT NOT NULL, y INT NOT NULL, z INT NOT NULL,"
                        + "type_id " + key() + ","
                        + "tier INT NOT NULL DEFAULT 1,"
                        + "stack_size INT NOT NULL DEFAULT 1,"
                        + "last_spawn BIGINT NOT NULL DEFAULT 0)",
                "CREATE INDEX IF NOT EXISTS idx_" + prefix + "gen_island ON " + prefix + "generators (island_id)",
                "CREATE INDEX IF NOT EXISTS idx_" + prefix + "spawn_island ON " + prefix + "spawners (island_id)");
    }

    /** Upsert statement for a table with the supplied primary key and columns. */
    public String upsert(String table, String primaryKey, List<String> columns) {
        String columnList = String.join(",", columns);
        String placeholders = columns.stream().map(column -> "?").reduce((a, b) -> a + "," + b).orElse("?");
        if (this == MYSQL) {
            StringBuilder updates = new StringBuilder();
            for (String column : columns) {
                if (column.equals(primaryKey)) {
                    continue;
                }
                if (updates.length() > 0) {
                    updates.append(',');
                }
                updates.append(column).append("=VALUES(").append(column).append(')');
            }
            return "INSERT INTO " + table + " (" + columnList + ") VALUES (" + placeholders + ")"
                    + " ON DUPLICATE KEY UPDATE " + updates;
        }
        return "INSERT OR REPLACE INTO " + table + " (" + columnList + ") VALUES (" + placeholders + ")";
    }

    public String driverClass() {
        return this == MYSQL ? "com.mysql.cj.jdbc.Driver" : "org.sqlite.JDBC";
    }
}
