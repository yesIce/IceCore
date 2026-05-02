package com.wiceh.icecore.common.exception;

public class RedisException extends IceCoreException {

    public RedisException(String message) {
        super(message);
    }

    public RedisException(String message, Throwable cause) {
        super(message, cause);
    }
}