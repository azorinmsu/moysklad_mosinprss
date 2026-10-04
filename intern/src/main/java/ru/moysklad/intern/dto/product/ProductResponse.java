package ru.moysklad.intern.dto.product;

import ru.moysklad.intern.entity.Modifier;

import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        String uom, // must be fetched as name
        String price, // from BigDecimal
        String currency, // same with uom
        List<Modifier> modifiers
) {  }
