package com.apexmc.skyblock.core.config;

/** Thrown when configuration is missing, malformed or fails validation. */
public class ConfigException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String path;

    public ConfigException(String path, String message) {
        super("[" + path + "] " + message);
        this.path = path;
    }

    public String path() {
        return path;
    }
}
