package ru.moysklad.intern.dto.uom;

import ru.moysklad.intern.entity.meta.UnitOfMeasurement;

import java.util.Objects;
import java.util.UUID;

public record UomDto(
        String name,
        String description,
        String externalId
) {
    public static UnitOfMeasurement to(UomDto uom) {
        return new UnitOfMeasurement(
                uom.name(),
                Objects.equals(uom.description(), "") ? null : uom.description(),
                UUID.fromString(uom.externalId())
        );
    }

    public static UomDto from(UnitOfMeasurement uom) {
        return new UomDto(
                uom.getName(),
                uom.getDescription(),
                uom.getExternalId().toString()
        );
    }
}
