package ru.moysklad.intern.utils.auth.exception;

public class AuthorizationRequiredException extends RuntimeException {
    public AuthorizationRequiredException(String message) {
        super(message);
    }
}
