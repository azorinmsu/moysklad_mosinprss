package ru.moysklad.intern.documentpatterns.validation;

import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;

import java.util.Objects;

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
        if (document.requiresSourceWarehouse() && document.getSourceWarehouse() == null)
            throw new DocumentValidationException(
                    DocumentErrorCode.MISSING_SOURCE_WAREHOUSE,
                    "Отсутствует склад-источник для документа со след. ID: " + document.getId()
            );

        if (document.requiresTargetWarehouse() && document.getTargetWarehouse() == null)
            throw new DocumentValidationException(
                    DocumentErrorCode.MISSING_TARGET_WAREHOUSE,
                    "Отсутствует склад-получатель для документа со след. ID: " + document.getId()
            );

        // + проверка, что у нас вообще указаны склады
        if (document.getSourceWarehouse() != null && document.getTargetWarehouse() != null
                && Objects.equals(document.getSourceWarehouse().getId(), document.getTargetWarehouse().getId()))
            throw new DocumentValidationException(
                    DocumentErrorCode.SAME_WAREHOUSES,
                    "Приход и отгрузка в один и тот же склад для документа: " + document.getId()
            );
    }
}
