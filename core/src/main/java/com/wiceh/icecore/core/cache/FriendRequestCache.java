package com.wiceh.icecore.core.cache;

import com.wiceh.icecore.common.exception.RedisException;
import com.wiceh.icecore.core.database.redis.RedisConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.SetParams;

import java.util.*;

public final class FriendRequestCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(FriendRequestCache.class);
    private static final String PREFIX = "ic:freq:";
    private static final long TTL_SECONDS = 300; // 5 minutes

    private final RedisConnectionProvider redisProvider;

    public FriendRequestCache(RedisConnectionProvider redisProvider) {
        this.redisProvider = Objects.requireNonNull(redisProvider);
    }

    public void put(UUID sender, UUID receiver) {
        try (Jedis jedis = redisProvider.getResource()) {
            jedis.set(key(sender, receiver), "1", SetParams.setParams().ex(TTL_SECONDS));
        } catch (JedisException e) {
            throw new RedisException("Failed to store friend request", e);
        }
    }

    public boolean exists(UUID sender, UUID receiver) {
        try (Jedis jedis = redisProvider.getResource()) {
            return jedis.exists(key(sender, receiver));
        } catch (JedisException e) {
            LOGGER.warn("Redis unavailable, assuming no friend request exists: {}", e.getMessage());
            return false;
        }
    }

    public void remove(UUID sender, UUID receiver) {
        try (Jedis jedis = redisProvider.getResource()) {
            jedis.del(key(sender, receiver));
        } catch (JedisException e) {
            LOGGER.warn("Redis unavailable, failed to remove friend request: {}", e.getMessage());
        }
    }

    public List<UUID> getIncomingRequests(UUID receiver) {
        try (Jedis jedis = redisProvider.getResource()) {
            Set<String> keys = jedis.keys(PREFIX + "*:" + receiver);
            List<UUID> senders = new ArrayList<>();
            for (String key : keys) {
                String[] parts = key.replace(PREFIX, "").split(":");
                if (parts.length == 2) {
                    try {
                        senders.add(UUID.fromString(parts[0]));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            return senders;
        } catch (JedisException e) {
            LOGGER.warn("Redis unavailable, returning empty incoming requests: {}", e.getMessage());
            return List.of();
        }
    }

    private String key(UUID sender, UUID receiver) {
        return PREFIX + sender + ":" + receiver;
    }
}