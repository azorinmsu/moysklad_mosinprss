package ru.moysklad.intern.documentpatterns.processing;

import ru.moysklad.intern.documentpatterns.decorator.DocumentOperation;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.StockMovement;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventPublisher;
import ru.moysklad.intern.documentpatterns.repository.StockRepository;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidator;

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
        // TODO:
        // 0. document == null — DocumentProcessingException с кодом DOCUMENT_REQUIRED.
        // 1. Проверить состояние: new DocumentContext(document).post().
        //    State проверит, что документ можно провести, и ЗАПЛАНИРУЕТ переход в POSTED.
        // 2. Бизнес-валидация:
        //    a) создайте ValidationContext(stockRepository, () -> calculateMovements(document));
        //    b) validationChain.validate(document, context) — общая цепочка;
        //    c) validateSpecific(document) — правила конкретного типа.
        // 3. Рассчитать движения: context.plannedMovements() — тот же список, что видела цепочка
        //    (стратегия вызывается один раз).
        // 4. Применить движения атомарно: stockRepository.apply(movements).
        //    Хранилище повторно проверит остатки: между валидацией и записью они могли измениться.
        // 5. Изменить статус: lifecycle.commit() — только после успешного шага 4.
        // 6. Зафиксировать результат: document.recordPostedMovements(movements) — нужно для отмены проведения.
        // 7. После фиксации опубликовать DocumentEvent.of(DOCUMENT_POSTED, document).
        //
        // Ошибки: при DocumentProcessingException на шагах 1–6 опубликуйте
        // DocumentEvent.failed(document, e.getCode()) и пробросьте исключение.
        // Статус и остатки при ошибке не должны измениться.
        throw new UnsupportedOperationException("TODO");
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
