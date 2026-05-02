package com.wiceh.icecore.core.repository.sql;

import com.wiceh.icecore.common.exception.DatabaseException;
import com.wiceh.icecore.common.model.PlayerProfile;
import com.wiceh.icecore.core.database.sql.SqlConnectionProvider;

import java.sql.*;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PlayerRepository {

    private static final String SELECT_BY_UUID = """
            SELECT uuid, username, first_login, last_login, playtime_millis, locale
            FROM players
            WHERE uuid = ?
            """;

    private static final String UPSERT = """
                INSERT INTO players (uuid, username, first_login, last_login, playtime_millis, locale)
                        VALUES (?, ?, ?, ?, ?, ?)
                        ON CONFLICT (uuid) DO UPDATE SET
                            username = EXCLUDED.username,
                            last_login = EXCLUDED.last_login,
                            playtime_millis = EXCLUDED.playtime_millis,
                            locale = EXCLUDED.locale
            """;

    private static final String UPDATE_LAST_LOGIN = """
            UPDATE players
            SET last_login = ?
            WHERE uuid = ?
            """;

    private static final String SELECT_BY_USERNAME = """
            SELECT uuid, username, first_login, last_login, playtime_millis, locale
            FROM players
            WHERE LOWER(username) = LOWER(?)
            """;

    private final SqlConnectionProvider connectionProvider;

    public PlayerRepository(SqlConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider,
                "connectionProvider must not be null");
    }

    public Optional<PlayerProfile> findByUuid(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_UUID)) {

            stmt.setObject(1, uuid);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find player by uuid: " + uuid, e);
        }
    }

    public void save(PlayerProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPSERT)) {

            stmt.setObject(1, profile.uuid());
            stmt.setString(2, profile.username());
            stmt.setTimestamp(3, Timestamp.from(profile.firstLogin()));
            stmt.setTimestamp(4, Timestamp.from(profile.lastLogin()));
            stmt.setLong(5, profile.playtimeMillis());
            stmt.setString(6, profile.locale());

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save player profile: " + profile.uuid(), e);
        }
    }

    public Optional<PlayerProfile> findByUsername(String username) {
        Objects.requireNonNull(username, "username must not be null");

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_USERNAME)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find player by username: " + username, e);
        }
    }

    public void updateLastLogin(UUID uuid, Instant lastLogin) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        Objects.requireNonNull(lastLogin, "lastLogin must not be null");

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_LAST_LOGIN)) {

            stmt.setTimestamp(1, Timestamp.from(lastLogin));
            stmt.setObject(2, uuid);

            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update last login for: " + uuid, e);
        }
    }

    private PlayerProfile mapRow(ResultSet rs) throws SQLException {
        return new PlayerProfile(
                rs.getObject("uuid", UUID.class),
                rs.getString("username"),
                rs.getTimestamp("first_login").toInstant(),
                rs.getTimestamp("last_login").toInstant(),
                rs.getLong("playtime_millis"),
                rs.getString("locale")
        );
    }
}