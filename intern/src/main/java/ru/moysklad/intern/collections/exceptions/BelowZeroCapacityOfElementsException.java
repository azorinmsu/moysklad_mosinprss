package ru.moysklad.intern.collections.exceptions;

public class BelowZeroCapacityOfElementsException extends IllegalArgumentException {
    public BelowZeroCapacityOfElementsException(String message) {
        super(message);
    }
}
