package ru.moysklad.intern.dto.product;

import ru.moysklad.intern.entity.Attribute;
import ru.moysklad.intern.entity.Modifier;

import java.util.List;

// самый полный DTO для создания
public record ProductAddNewRequest(
        String name,
        String description,
        String uom,
        String currency,
        String price, // danger zone because of [BigDecimal]
        List<Modifier> modifiers // will be returned with attrs
) { }
