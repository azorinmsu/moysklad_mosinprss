package ru.moysklad.intern.dto.currency;

import ru.moysklad.intern.entity.meta.Currency;

import java.util.UUID;

public record CurrencyDto(
        String name,
        String externalId
) {
    public static Currency to(CurrencyDto currency) {
        return new Currency(
                currency.name(),
                UUID.fromString(currency.externalId())
        );
    }
    public static CurrencyDto from(Currency currency) {
        return new CurrencyDto(
                currency.getName(),
                currency.getExternalId().toString()
        );
    }
}
