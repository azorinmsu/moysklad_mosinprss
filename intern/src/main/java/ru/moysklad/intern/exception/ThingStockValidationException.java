package ru.moysklad.intern.exception;

import java.util.Set;

public class ThingStockValidationException extends RuntimeException {

    private final Set<String> serials;

    public ThingStockValidationException(String message, Set<String> serials) {
        super(message);
        this.serials = Set.copyOf(serials);
    }

    public Set<String> getSerials() {
        return serials;
    }
}
