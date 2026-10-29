package ru.moysklad.intern.patterns.validation;

public enum ValidationErrorCode {

    /** В заказе нет ни одной позиции. */
    EMPTY_ORDER,

    /** Количество позиции не задано, равно нулю или отрицательное. */
    INVALID_QUANTITY,

    /** Цена позиции не задана или отрицательная. */
    INVALID_PRICE,

    /** Стоимость товаров превышает допустимый максимум. */
    TOTAL_AMOUNT_EXCEEDED
}
