package ru.moysklad.intern.utils.auth.exception;

public class IncorrectCredentialsException extends RuntimeException {
    public IncorrectCredentialsException(String message) {
        super(message);
    }
}
