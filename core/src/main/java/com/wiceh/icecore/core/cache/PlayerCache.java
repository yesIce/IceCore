package com.wiceh.icecore.core.cache;

import com.wiceh.icecore.common.exception.RedisException;
import com.wiceh.icecore.common.model.PlayerProfile;
import com.wiceh.icecore.core.database.redis.RedisConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.SetParams;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PlayerCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerCache.class);

    private static final String KEY_PREFIX = "ic:player:";
    private static final long TTL_SECONDS = 1800; // 30 minutes

    private final RedisConnectionProvider redisProvider;

    public PlayerCache(RedisConnectionProvider redisProvider) {
        this.redisProvider = Objects.requireNonNull(redisProvider, "redisProvider must not be null");
    }

    public Optional<PlayerProfile> get(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        String key = key(uuid);

        try (Jedis jedis = redisProvider.getResource()) {
            String value = jedis.get(key);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(deserialize(value));
        } catch (JedisException e) {
            LOGGER.warn("Redis unavailable, cache miss for {}: {}", uuid, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.warn("Failed to deserialize cached profile for {}, treating as miss", uuid, e);
            return Optional.empty();
        }
    }

    public void put(PlayerProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        String key = key(profile.uuid());

        try (Jedis jedis = redisProvider.getResource()) {
            jedis.set(key, serialize(profile), SetParams.setParams().ex(TTL_SECONDS));
        } catch (JedisException e) {
            LOGGER.warn("Redis unavailable, skipping cache write for {}: {}",
                    profile.uuid(), e.getMessage());
        } catch (Exception e) {
            throw new RedisException("Failed to serialize profile for cache: " + profile.uuid(), e);
        }
    }

    public void invalidate(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        String key = key(uuid);

        try (Jedis jedis = redisProvider.getResource()) {
            jedis.del(key);
        } catch (JedisException e) {
            LOGGER.warn("Redis unavailable, failed to invalidate cache for {}: {}",
                    uuid, e.getMessage());
        }
    }

    private String key(UUID uuid) {
        return KEY_PREFIX + uuid;
    }

    private String serialize(PlayerProfile profile) {
        return profile.uuid()
                + "\t" + profile.username()
                + "\t" + profile.firstLogin()
                + "\t" + profile.lastLogin()
                + "\t" + profile.playtimeMillis()
                + "\t" + nullToEmpty(profile.locale());
    }

    private PlayerProfile deserialize(String value) {
        String[] parts = value.split("\t", -1);
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid cached profile format");
        }

        return new PlayerProfile(
                UUID.fromString(parts[0]),
                parts[1],
                Instant.parse(parts[2]),
                Instant.parse(parts[3]),
                Long.parseLong(parts[4]),
                emptyToNull(parts[5])
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }
}
