package com.wiceh.icecore.common.util;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

public final class UuidUtil {

    private UuidUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static byte[] toBytes(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid must not be null");

        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());

        return buffer.array();
    }

    public static UUID fromBytes(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes must not be null");

        if (bytes.length != 16) {
            throw new IllegalArgumentException(
                    "UUID byte array must be 16 bytes, got " + bytes.length
            );
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long mostSigBits = buffer.getLong();
        long leastSigBits = buffer.getLong();
        return new UUID(mostSigBits, leastSigBits);
    }
}
