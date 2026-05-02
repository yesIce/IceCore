package com.wiceh.icecore.common.model;

import java.time.Instant;
import java.util.UUID;

public record PlayerProfile(
        UUID uuid,
        String username,
        Instant firstLogin,
        Instant lastLogin,
        long playtimeMillis,
        String locale
) {
}
