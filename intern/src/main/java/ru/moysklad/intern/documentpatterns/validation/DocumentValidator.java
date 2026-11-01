package ru.moysklad.intern.documentpatterns.validation;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;

/**
 * Проверка документа перед проведением. Нарушение — {@link DocumentValidationException}.
 */
@FunctionalInterface
public interface DocumentValidator {

    void validate(ErpDocument document, ValidationContext context);
}
