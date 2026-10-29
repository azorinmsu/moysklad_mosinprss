package ru.moysklad.intern.patterns.validation;

/**
 * Бизнес-ошибка проверки заказа покупателя.
 */
public class OrderValidationException extends RuntimeException {

    private final ValidationErrorCode code;

    public OrderValidationException(ValidationErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ValidationErrorCode getCode() {
        return code;
    }
}
