package com.wiceh.icecore.core.database.redis;

import com.wiceh.icecore.common.exception.RedisException;
import com.wiceh.icecore.core.config.RedisConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.time.Duration;

public final class RedisConnectionProvider implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConnectionProvider.class);

    private final JedisPool pool;

    public RedisConnectionProvider(RedisConfig config) {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(config.poolSize());
        poolConfig.setMaxIdle(Math.max(1, config.poolSize() / 2));
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(false);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setMaxWait(Duration.ofMillis(config.timeoutMillis()));

        try {
            if (config.hasPassword()) {
                this.pool = new JedisPool(
                        poolConfig,
                        config.host(),
                        config.port(),
                        config.timeoutMillis(),
                        config.password(),
                        config.database()
                );
            } else {
                this.pool = new JedisPool(
                        poolConfig,
                        config.host(),
                        config.port(),
                        config.timeoutMillis(),
                        null,
                        config.database()
                );
            }
        } catch (JedisException e) {
            throw new RedisException("Failed to initialize Redis connection pool", e);
        }

        try (Jedis jedis = pool.getResource()) {
            String pong = jedis.ping();
            if (!"PONG".equalsIgnoreCase(pong)) {
                pool.close();
                throw new RedisException("Unexpected response from Redis PING: " + pong);
            }
            LOGGER.info("Redis connection pool initialized and reachable ({}:{}/{})",
                    config.host(), config.port(), config.database());
        } catch (JedisException e) {
            pool.close();
            throw new RedisException(
                    "Redis is not reachable at " + config.host() + ":" + config.port(), e
            );
        }
    }

    public Jedis getResource() {
        try {
            return pool.getResource();
        } catch (JedisException e) {
            throw new RedisException("Failed to acquire Redis connection", e);
        }
    }

    public JedisPool pool() {
        return pool;
    }

    @Override
    public void close() {
        if (!pool.isClosed()) {
            pool.close();
            LOGGER.info("Redis connection pool closed");
        }
    }
}