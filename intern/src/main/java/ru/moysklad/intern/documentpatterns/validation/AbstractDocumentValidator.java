package ru.moysklad.intern.documentpatterns.validation;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;

/**
 * Звено цепочки обязанностей. <b>Механика связывания готова</b>: {@link #validate} выполняет собственную
 * проверку {@link #check} и передаёт документ следующему звену. Ошибка (исключение) прерывает цепочку.
 *
 * <p>Студент реализует только {@link #check} в конкретных звеньях: одно звено — одно правило.
 */
public abstract class AbstractDocumentValidator implements DocumentValidator {

    private AbstractDocumentValidator next;

    /**
     * Связывает звено со следующим и возвращает следующее, чтобы строить цепочку «через точку»:
     * {@code first.linkWith(second).linkWith(third)}.
     */
    public <T extends AbstractDocumentValidator> T linkWith(T next) {
        if (next == this) {
            throw new IllegalArgumentException("Звено не может ссылаться само на себя");
        }
        this.next = next;
        return next;
    }

    public AbstractDocumentValidator next() {
        return next;
    }

    @Override
    public final void validate(ErpDocument document, ValidationContext context) {
        check(document, context);
        if (next != null) {
            next.validate(document, context);
        }
    }

    /** Одно бизнес-правило. При нарушении бросьте {@link DocumentValidationException} с кодом. */
    protected abstract void check(ErpDocument document, ValidationContext context);
}
