package com.wiceh.icecore.core.database.sql;

import com.wiceh.icecore.common.exception.DatabaseException;
import com.wiceh.icecore.core.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class SqlConnectionProvider implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlConnectionProvider.class);

    private final HikariDataSource dataSource;

    public SqlConnectionProvider(DatabaseConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.jdbcUrl());
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setUsername(config.username());
        hikariConfig.setPassword(config.password());
        hikariConfig.setMaximumPoolSize(config.poolSize());
        hikariConfig.setPoolName("IceCore-SQL");

        hikariConfig.setConnectionTimeout(10_000);
        hikariConfig.setIdleTimeout(600_000);
        hikariConfig.setMaxLifetime(1_800_000);

        hikariConfig.addDataSourceProperty("prepareThreshold", "3");
        hikariConfig.addDataSourceProperty("preparedStatementCacheQueries", "256");
        hikariConfig.addDataSourceProperty("preparedStatementCacheSizeMiB", "5");
        hikariConfig.addDataSourceProperty("reWriteBatchedInserts", "true");

        this.dataSource = new HikariDataSource(hikariConfig);

        try (Connection conn = dataSource.getConnection()) {
            LOGGER.info("SQL connection pool initialized and database reachable ({})", config.jdbcUrl());
        } catch (SQLException e) {
            dataSource.close();
            throw new DatabaseException("Database is not reachable: " + config.jdbcUrl(), e);
        }
    }

    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to acquire SQL connection", e);
        }
    }

    public DataSource dataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        if (!dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("SQL connection pool closed");
        }
    }
}