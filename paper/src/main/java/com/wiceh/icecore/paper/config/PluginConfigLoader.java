package com.wiceh.icecore.paper.config;

import com.wiceh.icecore.core.config.DatabaseConfig;
import com.wiceh.icecore.core.config.RedisConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

public final class PluginConfigLoader {

    private final FileConfiguration config;

    public PluginConfigLoader(FileConfiguration config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
    }

    public DatabaseConfig loadDatabaseConfig() {
        ConfigurationSection section = section("database");
        return new DatabaseConfig(
                section.getString("host", "localhost"),
                section.getInt("port", 5432),
                section.getString("database", "icecore"),
                section.getString("username", "postgres"),
                section.getString("password", ""),
                section.getInt("pool-size", 10)
        );
    }

    public RedisConfig loadRedisConfig() {
        ConfigurationSection section = section("redis");
        String password = section.getString("password", "");
        return new RedisConfig(
                section.getString("host", "localhost"),
                section.getInt("port", 6379),
                password.isBlank() ? null : password,
                section.getInt("database", 0),
                section.getInt("pool-size", 8),
                section.getInt("timeout-millis", 2000)
        );
    }

    private ConfigurationSection section(String name) {
        ConfigurationSection s = config.getConfigurationSection(name);
        if (s == null) {
            throw new IllegalStateException(
                    "Missing required config section: " + name
            );
        }
        return s;
    }
}