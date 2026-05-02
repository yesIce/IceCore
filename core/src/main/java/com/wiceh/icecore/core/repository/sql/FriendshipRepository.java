package com.wiceh.icecore.core.repository.sql;

import com.wiceh.icecore.common.exception.DatabaseException;
import com.wiceh.icecore.common.model.Friendship;
import com.wiceh.icecore.core.database.sql.SqlConnectionProvider;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class FriendshipRepository {

    private static final String INSERT = """
            INSERT INTO friendships (first_uuid, second_uuid, since)
            VALUES (?, ?, ?)
            ON CONFLICT (first_uuid, second_uuid) DO NOTHING
            """;

    private static final String DELETE = """
            DELETE FROM friendships
            WHERE (first_uuid = ? AND second_uuid = ?)
               OR (first_uuid = ? AND second_uuid = ?)
            """;

    private static final String EXISTS = """
            SELECT 1 FROM friendships
            WHERE (first_uuid = ? AND second_uuid = ?)
               OR (first_uuid = ? AND second_uuid = ?)
            """;

    private static final String FIND_ALL = """
            SELECT first_uuid, second_uuid, since FROM friendships
            WHERE first_uuid = ? OR second_uuid = ?
            """;

    private final SqlConnectionProvider connectionProvider;

    public FriendshipRepository(SqlConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(connectionProvider);
    }

    public void save(Friendship friendship) {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT)) {

            stmt.setObject(1, friendship.firstUuid());
            stmt.setObject(2, friendship.secondUuid());
            stmt.setTimestamp(3, Timestamp.from(friendship.since()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save friendship", e);
        }
    }

    public void delete(UUID firstUuid, UUID secondUuid) {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE)) {

            stmt.setObject(1, firstUuid);
            stmt.setObject(2, secondUuid);
            stmt.setObject(3, secondUuid);
            stmt.setObject(4, firstUuid);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete friendship", e);
        }
    }

    public boolean exists(UUID firstUuid, UUID secondUuid) {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(EXISTS)) {

            stmt.setObject(1, firstUuid);
            stmt.setObject(2, secondUuid);
            stmt.setObject(3, secondUuid);
            stmt.setObject(4, firstUuid);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check friendship existence", e);
        }
    }

    public List<Friendship> findAll(UUID uuid) {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(FIND_ALL)) {

            stmt.setObject(1, uuid);
            stmt.setObject(2, uuid);

            try (ResultSet rs = stmt.executeQuery()) {
                List<Friendship> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new Friendship(
                            rs.getObject("first_uuid", UUID.class),
                            rs.getObject("second_uuid", UUID.class),
                            rs.getTimestamp("since").toInstant()
                    ));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find friendships", e);
        }
    }
}