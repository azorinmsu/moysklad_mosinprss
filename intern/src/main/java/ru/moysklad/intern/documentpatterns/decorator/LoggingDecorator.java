package ru.moysklad.intern.documentpatterns.decorator;

import ru.moysklad.intern.documentpatterns.model.DocumentProcessingException;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Логирует начало, успех и ошибку операции. Бизнес-результат не меняет.
 */
public class LoggingDecorator extends DocumentOperationDecorator {

    private final Consumer<String> log;

    public LoggingDecorator(DocumentOperation delegate, Consumer<String> log) {
        super(delegate);
        this.log = Objects.requireNonNull(log, "log");
    }

    @Override
    public void execute(ErpDocument document) {
        log.accept(String.format(
                "Начало: %s (ID: %s). Статус: %s",
                document.getType(),
                document.getId(),
                document.getStatus().name()));

        try {
            delegate.execute(document);

            log.accept(String.format(
                    "Успешно: %s (ID: %s). Статус: %s",
                    document.getType(),
                    document.getId(),
                    document.getStatus().name()
            ));
        } catch (RuntimeException e) {
            log.accept(String.format(
                    "Произошла ошибка с документом (ID: %s): %s",
                    document.getId(),
                    e instanceof DocumentProcessingException dpe ? dpe.getCode() : e.getMessage()
            ));
            throw e;
        }
    }
}
