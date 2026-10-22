package ru.moysklad.intern.remapstub.service;

import org.springframework.http.HttpStatus;

public class RemapStubException extends RuntimeException {

    private final HttpStatus status;
    private final int errorCode;

    public RemapStubException(HttpStatus status, int errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public static RemapStubException notFound(String message) {
        return new RemapStubException(HttpStatus.NOT_FOUND, 1021, message);
    }

    public static RemapStubException badRequest(String message) {
        return new RemapStubException(HttpStatus.BAD_REQUEST, 3000, message);
    }

    public static RemapStubException conflict(String message) {
        return new RemapStubException(HttpStatus.CONFLICT, 3006, message);
    }
}
