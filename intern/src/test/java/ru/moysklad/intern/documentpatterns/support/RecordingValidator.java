package ru.moysklad.intern.documentpatterns.support;

import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.validation.AbstractDocumentValidator;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidationException;
import ru.moysklad.intern.documentpatterns.validation.ValidationContext;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Тестовое звено цепочки: считает вызовы и при необходимости бросает ошибку с заданным кодом.
 * Поставьте его после проверяемого звена, чтобы доказать, что цепочка остановилась.
 */
public class RecordingValidator extends AbstractDocumentValidator {

    private final AtomicInteger invocations = new AtomicInteger();
    private final DocumentErrorCode failWith;

    public RecordingValidator() {
        this(null);
    }

    private RecordingValidator(DocumentErrorCode failWith) {
        this.failWith = failWith;
    }

    public static RecordingValidator failingWith(DocumentErrorCode code) {
        return new RecordingValidator(code);
    }

    @Override
    protected void check(ErpDocument document, ValidationContext context) {
        invocations.incrementAndGet();
        if (failWith != null) {
            throw new DocumentValidationException(failWith, "Тестовая ошибка " + failWith);
        }
    }

    public int invocations() {
        return invocations.get();
    }
}
