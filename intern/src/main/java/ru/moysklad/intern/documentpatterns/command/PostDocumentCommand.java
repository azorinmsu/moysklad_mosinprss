package ru.moysklad.intern.documentpatterns.command;

import ru.moysklad.intern.documentpatterns.decorator.DocumentOperation;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;

import java.util.Objects;
import java.util.UUID;

/**
 * Команда проведения документа.
 *
 * <p>{@code postingOperation} — обработчик проведения нужного типа, обычно обёрнутый декораторами
 * (логирование, аудит). Команда не знает, как именно проводится документ.
 */
public class PostDocumentCommand implements DocumentCommand {

    private final UUID operationId;
    private final ErpDocument document;
    private final DocumentOperation postingOperation;

    public PostDocumentCommand(UUID operationId, ErpDocument document, DocumentOperation postingOperation) {
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.document = Objects.requireNonNull(document, "document");
        this.postingOperation = Objects.requireNonNull(postingOperation, "postingOperation");
    }

    @Override
    public UUID operationId() {
        return operationId;
    }

    @Override
    public String name() {
        return "POST";
    }

    @Override
    public ErpDocument document() {
        return document;
    }

    @Override
    public void execute() {
        // TODO: проведите документ через postingOperation.
        // Проверки состояния, валидация, движения и события — внутри обработчика (Template Method);
        // не дублируйте их в команде.
        throw new UnsupportedOperationException("TODO");
    }
}
