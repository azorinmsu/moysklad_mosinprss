package ru.moysklad.intern.exception.aggregate;

/**
 * Бизнес-исключение: недопустимое значение цены.
 */
public class InvalidPriceException extends RuntimeException {

    public InvalidPriceException(String message) {
        super(message);
    }
}
