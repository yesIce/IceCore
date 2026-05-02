package com.wiceh.icecore.core.config;

import java.util.Objects;

public record RedisConfig(
        String host,
        int port,
        String password,
        int database,
        int poolSize,
        int timeoutMillis
) {

    public RedisConfig {
        Objects.requireNonNull(host, "host must not be null");

        if (port < 1 || port > 65535)
            throw new IllegalArgumentException("Invalid port: " + port);

        if (database < 0 || database > 15)
            throw new IllegalArgumentException("Database must be between 0 and 15");

        if (poolSize < 1)
            throw new IllegalArgumentException("Pool size must be at least 1");

        if (timeoutMillis < 1)
            throw new IllegalArgumentException("Timeout must be at least 1ms");
    }

    public boolean hasPassword() {
        return password != null && !password.isBlank();
    }
}