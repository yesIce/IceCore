package com.wiceh.icecore.core.api.service;

import com.wiceh.icecore.common.enums.FriendActionResult;
import com.wiceh.icecore.common.model.Friendship;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface FriendService {

    CompletableFuture<FriendActionResult> sendRequest(UUID sender, UUID receiver);

    CompletableFuture<FriendActionResult> acceptRequest(UUID accepter, UUID sender);

    CompletableFuture<FriendActionResult> denyRequest(UUID denier, UUID sender);

    CompletableFuture<FriendActionResult> removeFriend(UUID requester, UUID target);

    CompletableFuture<List<Friendship>> getFriends(UUID uuid);

    CompletableFuture<List<UUID>> getIncomingRequests(UUID uuid);
}