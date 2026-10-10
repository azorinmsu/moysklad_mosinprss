package ru.moysklad.intern.dto.product;

// самый полный DTO для создания
public record ProductRequest(
        String name,
        String description,
        String uom,
        String currency,
        String price // danger zone because of [BigDecimal]
) { }
