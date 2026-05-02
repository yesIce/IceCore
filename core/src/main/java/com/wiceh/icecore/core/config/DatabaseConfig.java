package com.wiceh.icecore.core.config;

import java.util.Objects;

public record DatabaseConfig(
        String host,
        int port,
        String database,
        String username,
        String password,
        int poolSize
) {

    public DatabaseConfig {
        Objects.requireNonNull(host, "host must not be null");
        Objects.requireNonNull(database, "database must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(password, "password must not be null");

        if (port < 1 || port > 65535)
            throw new IllegalArgumentException("Invalid port: " + port);

        if (poolSize < 1)
            throw new IllegalArgumentException("Pool size must be at least 1");
    }

    public String jdbcUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
    }
}