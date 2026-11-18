package ru.moysklad.intern.documentpatterns.processing;

import ru.moysklad.intern.documentpatterns.decorator.DocumentOperation;
import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.DocumentProcessingException;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.StockMovement;
import ru.moysklad.intern.documentpatterns.observer.DocumentEvent;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventPublisher;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventType;
import ru.moysklad.intern.documentpatterns.repository.StockRepository;
import ru.moysklad.intern.documentpatterns.state.DocumentContext;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidator;
import ru.moysklad.intern.documentpatterns.validation.ValidationContext;

import java.util.List;
import java.util.Objects;

/**
 * Паттерн Template Method: общий алгоритм проведения документа.
 *
 * <p>Последовательность шагов зафиксирована в {@code final}-методе {@link #process(ErpDocument)};
 * подклассы переопределяют только «крючки» {@link #validateSpecific} и {@link #calculateMovements}.
 *
 * <p>Обработчик сам является базовой {@link DocumentOperation}, которую можно обернуть декораторами.
 */
public abstract class AbstractDocumentProcessor implements DocumentOperation {

    protected final DocumentValidator validationChain;
    protected final StockRepository stockRepository;
    protected final DocumentEventPublisher eventPublisher;

    protected AbstractDocumentProcessor(ProcessingInfrastructure infrastructure) {
        Objects.requireNonNull(infrastructure, "infrastructure");
        this.validationChain = infrastructure.validationChain();
        this.stockRepository = infrastructure.stockRepository();
        this.eventPublisher = infrastructure.eventPublisher();
    }

    /**
     * Проводит документ. Порядок шагов определён здесь и только здесь.
     */
    public final void process(ErpDocument document) {
        if (document == null)
            throw new DocumentProcessingException(DocumentErrorCode.DOCUMENT_REQUIRED, "Документ не передан");

        DocumentContext ctx = new DocumentContext(document);

        try {
            ctx.post(); // проверка жизненного цикла

            // валидация
            ValidationContext valCtx = new ValidationContext(stockRepository, () -> calculateMovements(document));
            validationChain.validate(document, valCtx);
            validateSpecific(document);

            // плановые движения
            List<StockMovement> moves = valCtx.plannedMovements();

            stockRepository.apply(moves);

            ctx.commit();

            document.recordPostedMovements(moves);

            eventPublisher.publish(DocumentEvent.of(DocumentEventType.DOCUMENT_POSTED, document));


        } catch (DocumentProcessingException e) {
            eventPublisher.publish(DocumentEvent.failed(document, e.getCode()));
            throw e;
        }
    }

    @Override
    public final void execute(ErpDocument document) {
        process(document);
    }

    /** Правила, специфичные для типа документа (дополнение к общей цепочке). */
    protected abstract void validateSpecific(ErpDocument document);

    /** Расчёт движений для типа документа (обычно — делегирование стратегии). */
    protected abstract List<StockMovement> calculateMovements(ErpDocument document);
}
