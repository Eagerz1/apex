package com.apexmc.skyblock.core.config;

import java.util.ArrayList;
import java.util.List;

/** Collected configuration problems produced during startup validation. */
public final class ValidationReport {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void error(String path, String message) {
        errors.add("[" + path + "] " + message);
    }

    public void warn(String path, String message) {
        warnings.add("[" + path + "] " + message);
    }

    public List<String> errors() {
        return List.copyOf(errors);
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /** Throws a {@link ConfigException} describing every error found. */
    public void throwIfInvalid() {
        if (hasErrors()) {
            throw new ConfigException("config", "configuration is invalid:\n - " + String.join("\n - ", errors));
        }
    }
}
