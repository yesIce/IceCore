package com.wiceh.icecore.common.exception;

public class IceCoreException extends RuntimeException {

    public IceCoreException(String message) {
        super(message);
    }

    public IceCoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
