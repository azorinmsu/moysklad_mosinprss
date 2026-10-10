package ru.moysklad.intern.exception;

import java.util.Set;

public class ThingNotInStockException extends ThingStockValidationException {

    public ThingNotInStockException(Set<String> serials) {
        super("Серийных номеров нет на складе: " + serials, serials);
    }
}
