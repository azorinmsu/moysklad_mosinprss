package ru.moysklad.intern.dto.product;

import ru.moysklad.intern.entity.Modifier;
import ru.moysklad.intern.entity.meta.Currency;
import ru.moysklad.intern.entity.meta.UnitOfMeasurement;

import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        UnitOfMeasurement uom, // must be fetched as name
        String price, // from BigDecimal
        Currency currency, // same with uom
        List<Modifier> modifiers
) {  }
