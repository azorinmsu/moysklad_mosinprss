package ru.moysklad.intern.documentpatterns.validation;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;

/**
 * Правило: склады документа указаны корректно.
 *
 * <p>Какие склады нужны, сообщает сам документ ({@code requiresSourceWarehouse()},
 * {@code requiresTargetWarehouse()}), поэтому звено не содержит {@code switch} по типу документа
 * и не меняется при добавлении нового типа.
 */
public class WarehouseValidator extends AbstractDocumentValidator {

    @Override
    protected void check(ErpDocument document, ValidationContext context) {
        // TODO:
        // 1. Требуется склад-источник, но он не указан — MISSING_SOURCE_WAREHOUSE.
        // 2. Требуется склад-получатель, но он не указан — MISSING_TARGET_WAREHOUSE.
        // 3. Указаны оба склада и это один и тот же склад — SAME_WAREHOUSES.
        //    Warehouse не переопределяет equals: сравнивайте идентификаторы (getId()).
        throw new UnsupportedOperationException("TODO");
    }
}
