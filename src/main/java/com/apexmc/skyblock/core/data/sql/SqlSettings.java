package com.apexmc.skyblock.core.data.sql;

import com.apexmc.skyblock.core.config.Cfg;
import com.apexmc.skyblock.core.config.ConfigException;

/**
 * Storage settings.
 *
 * @param dialect      SQLite (default) or MySQL/MariaDB
 * @param jdbcUrl      fully built JDBC url
 * @param username     database user (MySQL only)
 * @param password     database password (MySQL only)
 * @param tablePrefix  table prefix
 * @param saveIntervalSeconds autosave interval for dirty profiles
 */
public record SqlSettings(SqlDialect dialect, String jdbcUrl, String username, String password,
                          String tablePrefix, int saveIntervalSeconds) {

    public static SqlSettings fromConfig(Cfg cfg, String dataFolderPath) {
        SqlDialect dialect;
        String type = cfg.getString("type", "sqlite");
        try {
            dialect = SqlDialect.fromId(type);
        } catch (IllegalArgumentException ex) {
            throw new ConfigException(cfg.path() + ".type", ex.getMessage());
        }
        String prefix = cfg.getString("table-prefix", "apexsb_");
        if (!prefix.matches("[A-Za-z0-9_]*")) {
            throw new ConfigException(cfg.path() + ".table-prefix", "may only contain letters, digits and underscores");
        }
        int interval = cfg.getInt("save-interval-seconds", 300);
        if (interval < 30) {
            throw new ConfigException(cfg.path() + ".save-interval-seconds", "must be at least 30 seconds");
        }
        if (dialect == SqlDialect.SQLITE) {
            String file = cfg.getString("file", "progression.db");
            return new SqlSettings(dialect, "jdbc:sqlite:" + dataFolderPath + "/" + file, "", "", prefix, interval);
        }
        Cfg mysql = cfg.section("mysql");
        String host = mysql.getString("host", "localhost");
        int port = mysql.getInt("port", 3306);
        String database = mysql.getString("database", "skyblock");
        String extra = mysql.getString("parameters", "useSSL=false&characterEncoding=utf8");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + (extra.isBlank() ? "" : "?" + extra);
        return new SqlSettings(dialect, url, mysql.getString("username", "root"),
                mysql.getString("password", ""), prefix, interval);
    }
}
