package com.apexmc.skyblock.core.data.sql;

/** Wraps SQL failures with plugin context so they are never silently swallowed. */
public class StorageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
