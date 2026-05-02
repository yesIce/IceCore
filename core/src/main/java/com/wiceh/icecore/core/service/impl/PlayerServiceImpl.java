package com.wiceh.icecore.core.service.impl;

import com.wiceh.icecore.common.model.PlayerProfile;
import com.wiceh.icecore.core.api.service.PlayerService;
import com.wiceh.icecore.core.cache.PlayerCache;
import com.wiceh.icecore.core.repository.sql.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class PlayerServiceImpl implements PlayerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerServiceImpl.class);

    private final PlayerRepository repository;
    private final PlayerCache cache;
    private final Executor executor;

    public PlayerServiceImpl(PlayerRepository repository, PlayerCache cache, Executor executor) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.cache = Objects.requireNonNull(cache, "cache must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
    }

    @Override
    public CompletableFuture<Optional<PlayerProfile>> loadProfile(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");

        return CompletableFuture.supplyAsync(() -> {
            Optional<PlayerProfile> cached = cache.get(uuid);
            if (cached.isPresent()) {
                LOGGER.debug("Cache HIT for {}", uuid);
                return cached;
            }

            LOGGER.debug("Cache MISS for {}, hitting SQL", uuid);
            Optional<PlayerProfile> fromDb = repository.findByUuid(uuid);

            fromDb.ifPresent(cache::put);

            return fromDb;
        }, executor);
    }

    @Override
    public CompletableFuture<PlayerProfile> handleJoin(UUID uuid, String username) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        Objects.requireNonNull(username, "username must not be null");

        return CompletableFuture.supplyAsync(() -> {
            Instant now = Instant.now();

            Optional<PlayerProfile> existing = repository.findByUuid(uuid);

            PlayerProfile profile;
            if (existing.isPresent()) {
                PlayerProfile current = existing.get();
                profile = new PlayerProfile(
                        current.uuid(),
                        username,
                        current.firstLogin(),
                        now,
                        current.playtimeMillis(),
                        current.locale()
                );
                LOGGER.debug("Updating existing profile for {}", username);
            } else {
                profile = new PlayerProfile(uuid, username, now, now, 0L, null);
                LOGGER.info("Creating new profile for {} ({})", username, uuid);
            }

            repository.save(profile);
            cache.put(profile);
            return profile;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> handleQuit(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");

        return CompletableFuture.runAsync(() -> {
            Optional<PlayerProfile> existing = cache.get(uuid);
            if (existing.isEmpty()) {
                existing = repository.findByUuid(uuid);
            }

            if (existing.isEmpty()) {
                LOGGER.warn("handleQuit called for unknown player: {}", uuid);
                return;
            }

            PlayerProfile current = existing.get();
            Instant now = Instant.now();
            long sessionMillis = Math.max(0L,
                    java.time.Duration.between(current.lastLogin(), now).toMillis()
            );

            PlayerProfile updated = new PlayerProfile(
                    current.uuid(),
                    current.username(),
                    current.firstLogin(),
                    current.lastLogin(),
                    current.playtimeMillis() + sessionMillis,
                    current.locale()
            );

            repository.save(updated);
            cache.invalidate(uuid);

            LOGGER.debug("Saved quit for {}: +{}ms playtime", current.username(), sessionMillis);
        }, executor);
    }

    @Override
    public CompletableFuture<Void> saveProfile(PlayerProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");

        return CompletableFuture.runAsync(() -> {
            repository.save(profile);
            cache.invalidate(profile.uuid());
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<PlayerProfile>> findByUsername(String username) {
        Objects.requireNonNull(username, "username must not be null");
        return CompletableFuture.supplyAsync(
                () -> repository.findByUsername(username),
                executor
        );
    }

    @Override
    public CompletableFuture<Void> updateLocale(UUID uuid, String locale) {
        Objects.requireNonNull(uuid, "uuid must not be null");
        Objects.requireNonNull(locale, "locale must not be null");

        return CompletableFuture.runAsync(() -> {
            Optional<PlayerProfile> existing = cache.get(uuid);
            if (existing.isEmpty()) {
                existing = repository.findByUuid(uuid);
            }

            if (existing.isEmpty()) {
                throw new IllegalStateException("Profile not found for uuid: " + uuid);
            }

            PlayerProfile current = existing.get();
            PlayerProfile updated = new PlayerProfile(
                    current.uuid(),
                    current.username(),
                    current.firstLogin(),
                    current.lastLogin(),
                    current.playtimeMillis(),
                    locale
            );

            repository.save(updated);
            cache.put(updated);
        }, executor);
    }
}
