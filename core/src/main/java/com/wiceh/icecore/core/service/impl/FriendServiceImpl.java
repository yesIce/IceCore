package com.wiceh.icecore.core.service.impl;

import com.wiceh.icecore.common.enums.FriendActionResult;
import com.wiceh.icecore.common.model.Friendship;
import com.wiceh.icecore.core.api.service.FriendService;
import com.wiceh.icecore.core.cache.FriendRequestCache;
import com.wiceh.icecore.core.repository.sql.FriendshipRepository;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class FriendServiceImpl implements FriendService {

    private final FriendshipRepository repository;
    private final FriendRequestCache requestCache;
    private final Executor executor;

    public FriendServiceImpl(FriendshipRepository repository,
                             FriendRequestCache requestCache,
                             Executor executor) {
        this.repository = Objects.requireNonNull(repository);
        this.requestCache = Objects.requireNonNull(requestCache);
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    public CompletableFuture<FriendActionResult> sendRequest(UUID sender, UUID receiver) {
        Objects.requireNonNull(sender);
        Objects.requireNonNull(receiver);

        return CompletableFuture.supplyAsync(() -> {
            if (sender.equals(receiver)) {
                return FriendActionResult.CANNOT_ADD_SELF;
            }
            if (repository.exists(sender, receiver)) {
                return FriendActionResult.ALREADY_FRIENDS;
            }
            if (requestCache.exists(receiver, sender)) {
                requestCache.remove(receiver, sender);
                repository.save(Friendship.of(sender, receiver, Instant.now()));
                return FriendActionResult.AUTO_ACCEPTED;
            }
            requestCache.put(sender, receiver);
            return FriendActionResult.REQUEST_SENT;
        }, executor);
    }

    @Override
    public CompletableFuture<FriendActionResult> acceptRequest(UUID accepter, UUID sender) {
        return CompletableFuture.supplyAsync(() -> {
            if (!requestCache.exists(sender, accepter)) {
                return FriendActionResult.REQUEST_NOT_FOUND;
            }
            requestCache.remove(sender, accepter);
            repository.save(Friendship.of(sender, accepter, Instant.now()));
            return FriendActionResult.REQUEST_ACCEPTED;
        }, executor);
    }

    @Override
    public CompletableFuture<FriendActionResult> denyRequest(UUID denier, UUID sender) {
        return CompletableFuture.supplyAsync(() -> {
            if (!requestCache.exists(sender, denier)) {
                return FriendActionResult.REQUEST_NOT_FOUND;
            }
            requestCache.remove(sender, denier);
            return FriendActionResult.REQUEST_DENIED;
        }, executor);
    }

    @Override
    public CompletableFuture<FriendActionResult> removeFriend(UUID requester, UUID target) {
        return CompletableFuture.supplyAsync(() -> {
            if (!repository.exists(requester, target)) {
                return FriendActionResult.NOT_FRIENDS;
            }
            repository.delete(requester, target);
            return FriendActionResult.FRIEND_REMOVED;
        }, executor);
    }

    @Override
    public CompletableFuture<List<Friendship>> getFriends(UUID uuid) {
        return CompletableFuture.supplyAsync(
                () -> repository.findAll(uuid), executor);
    }

    @Override
    public CompletableFuture<List<UUID>> getIncomingRequests(UUID uuid) {
        return CompletableFuture.supplyAsync(
                () -> requestCache.getIncomingRequests(uuid), executor);
    }
}