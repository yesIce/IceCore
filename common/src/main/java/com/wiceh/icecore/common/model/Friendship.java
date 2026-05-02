package com.wiceh.icecore.common.model;

import java.time.Instant;
import java.util.UUID;

public record Friendship(
        UUID firstUuid,
        UUID secondUuid,
        Instant since
) {

    public UUID otherThan(UUID uuid) {
        if (uuid.equals(firstUuid)) return secondUuid;
        if (uuid.equals(secondUuid)) return firstUuid;
        throw new IllegalArgumentException(uuid + " is not part of this friendship");
    }

    public static Friendship of(UUID firstUuid, UUID secondUuid, Instant since) {
        if (firstUuid.compareTo(secondUuid) <= 0)
            return new Friendship(firstUuid, secondUuid, since);
        return new Friendship(secondUuid, firstUuid, since);
    }
}