package com.wiceh.icecore.common.exception;

public class DatabaseException extends IceCoreException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
