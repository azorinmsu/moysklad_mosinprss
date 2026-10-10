package ru.moysklad.intern.exception;

import java.util.Set;

public class ThingAlreadyInStockException extends ThingStockValidationException {

    public ThingAlreadyInStockException(Set<String> serials) {
        super("Серийные номера уже есть на складе: " + serials, serials);
    }
}
