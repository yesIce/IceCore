package com.wiceh.icecore.core.api.service;

import com.wiceh.icecore.common.model.PlayerProfile;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerService {

    CompletableFuture<Optional<PlayerProfile>> loadProfile(UUID uuid);

    CompletableFuture<PlayerProfile> handleJoin(UUID uuid, String username);

    CompletableFuture<Void> handleQuit(UUID uuid);

    CompletableFuture<Void> saveProfile(PlayerProfile profile);

    CompletableFuture<Optional<PlayerProfile>> findByUsername(String username);

    CompletableFuture<Void> updateLocale(UUID uuid, String locale);
}
