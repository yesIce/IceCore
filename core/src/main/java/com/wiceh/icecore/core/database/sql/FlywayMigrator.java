package com.wiceh.icecore.core.database.sql;

import com.wiceh.icecore.common.exception.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

public final class FlywayMigrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlywayMigrator.class);
    private static final String MIGRATION_LOCATION = "db/migration";
    private static final Pattern MIGRATION_PATTERN = Pattern.compile("V([0-9][0-9._-]*)__([A-Za-z0-9._-]+)\\.sql");

    private final DataSource dataSource;

    public FlywayMigrator(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource must not be null");
    }

    public void migrate() {
        try {
            List<Migration> migrations = discoverMigrations();
            if (migrations.isEmpty()) {
                LOGGER.warn("No database migrations found in classpath:{}", MIGRATION_LOCATION);
                return;
            }

            int executed = 0;
            String currentVersion = null;

            try (Connection connection = dataSource.getConnection()) {
                ensureSchemaHistory(connection);
                Set<String> appliedVersions = appliedVersions(connection);

                for (Migration migration : migrations) {
                    if (appliedVersions.contains(migration.version())) {
                        currentVersion = migration.version();
                        continue;
                    }

                    applyMigration(connection, migration);
                    appliedVersions.add(migration.version());
                    currentVersion = migration.version();
                    executed++;
                }
            }

            if (executed == 0) {
                LOGGER.info("Database schema is up to date (version: {})", currentVersion);
            } else {
                LOGGER.info("Applied {} migration(s), schema now at version {}", executed, currentVersion);
            }
        } catch (Exception e) {
            throw new DatabaseException("Failed to run database migrations", e);
        }
    }

    private List<Migration> discoverMigrations() throws IOException, URISyntaxException {
        ClassLoader classLoader = FlywayMigrator.class.getClassLoader();
        List<Migration> migrations = new ArrayList<>();

        URL location = classLoader.getResource(MIGRATION_LOCATION);
        if (location == null) {
            return migrations;
        }

        if ("file".equals(location.getProtocol())) {
            try (var paths = Files.list(Path.of(location.toURI()))) {
                paths.filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .map(this::parseMigration)
                        .filter(Objects::nonNull)
                        .forEach(migrations::add);
            }
        } else if ("jar".equals(location.getProtocol())) {
            JarURLConnection connection = (JarURLConnection) location.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!entry.isDirectory() && name.startsWith(MIGRATION_LOCATION + "/")) {
                        Migration migration = parseMigration(name.substring(MIGRATION_LOCATION.length() + 1));
                        if (migration != null) {
                            migrations.add(migration);
                        }
                    }
                }
            }
        }

        migrations.sort(Comparator.comparing(Migration::version, this::compareVersions));
        return migrations;
    }

    private Migration parseMigration(String fileName) {
        Matcher matcher = MIGRATION_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            return null;
        }

        String version = matcher.group(1).replace('_', '.').replace('-', '.');
        String description = matcher.group(2).replace('_', ' ');
        return new Migration(version, description, fileName);
    }

    private int compareVersions(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);

        for (int i = 0; i < length; i++) {
            int leftValue = i < leftParts.length ? Integer.parseInt(leftParts[i]) : 0;
            int rightValue = i < rightParts.length ? Integer.parseInt(rightParts[i]) : 0;
            int comparison = Integer.compare(leftValue, rightValue);
            if (comparison != 0) {
                return comparison;
            }
        }

        return 0;
    }

    private void ensureSchemaHistory(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS flyway_schema_history (
                        installed_rank INT PRIMARY KEY,
                        version VARCHAR(50),
                        description VARCHAR(200) NOT NULL,
                        type VARCHAR(20) NOT NULL,
                        script VARCHAR(1000) NOT NULL,
                        checksum INT,
                        installed_by VARCHAR(100) NOT NULL,
                        installed_on TIMESTAMPTZ NOT NULL DEFAULT now(),
                        execution_time INT NOT NULL,
                        success BOOLEAN NOT NULL
                    )
                    """);
            statement.execute("""
                    CREATE INDEX IF NOT EXISTS flyway_schema_history_s_idx
                    ON flyway_schema_history (success)
                    """);
        }
    }

    private Set<String> appliedVersions(Connection connection) throws SQLException {
        Set<String> versions = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT version
                FROM flyway_schema_history
                WHERE success = TRUE AND version IS NOT NULL
                """);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                versions.add(resultSet.getString("version"));
            }
        }
        return versions;
    }

    private void applyMigration(Connection connection, Migration migration) throws SQLException, IOException {
        String sql = readMigrationSql(migration.fileName());
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        Instant startedAt = Instant.now();

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            insertSchemaHistory(connection, migration, checksum(sql), executionTimeMillis(startedAt));
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    private String readMigrationSql(String fileName) throws IOException {
        String resource = MIGRATION_LOCATION + "/" + fileName;
        try (InputStream input = FlywayMigrator.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Missing migration resource: " + resource);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void insertSchemaHistory(
            Connection connection,
            Migration migration,
            int checksum,
            int executionTime
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO flyway_schema_history (
                    installed_rank, version, description, type, script,
                    checksum, installed_by, execution_time, success
                )
                VALUES (
                    (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history),
                    ?, ?, 'SQL', ?, ?, ?, ?, TRUE
                )
                """)) {

            statement.setString(1, migration.version());
            statement.setString(2, migration.description());
            statement.setString(3, migration.fileName());
            statement.setInt(4, checksum);
            statement.setString(5, installedBy(connection));
            statement.setInt(6, executionTime);
            statement.executeUpdate();
        }
    }

    private int checksum(String sql) {
        CRC32 crc32 = new CRC32();
        crc32.update(sql.getBytes(StandardCharsets.UTF_8));
        return (int) crc32.getValue();
    }

    private int executionTimeMillis(Instant startedAt) {
        return Math.toIntExact(Math.max(0L, Duration.between(startedAt, Instant.now()).toMillis()));
    }

    private String installedBy(Connection connection) {
        try {
            String user = connection.getMetaData().getUserName();
            return user == null || user.isBlank() ? "icecore" : user;
        } catch (SQLException e) {
            return "icecore";
        }
    }

    private record Migration(String version, String description, String fileName) {
    }
}
