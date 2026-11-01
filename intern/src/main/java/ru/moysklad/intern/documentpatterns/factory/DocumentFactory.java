package ru.moysklad.intern.documentpatterns.factory;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.processing.AbstractDocumentProcessor;
import ru.moysklad.intern.documentpatterns.processing.ProcessingInfrastructure;
import ru.moysklad.intern.documentpatterns.strategy.StockMovementStrategy;
import ru.moysklad.intern.entity.operation.DocumentsType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Фабрика документов одного типа.
 *
 * <ul>
 *     <li><b>Factory Method</b>: {@link #createDocument} — общий алгоритм создания, а конкретный класс документа
 *     выбирает подкласс в {@link #newDocument};</li>
 *     <li><b>Abstract Factory</b>: фабрика создаёт согласованное семейство объектов одного типа документа —
 *     документ, стратегию движений и обработчик проведения. Нельзя случайно провести перемещение
 *     обработчиком отгрузки.</li>
 * </ul>
 *
 * <p>Новый тип документа = новый подкласс фабрики (+ документ, стратегия, обработчик) и регистрация
 * в {@code DocumentService}. Центральный сервис не меняется.
 */
public abstract class DocumentFactory {

    /** Общий алгоритм создания документа (Factory Method использует {@link #newDocument}). */
    public final ErpDocument createDocument(UUID id, LocalDateTime moment) {
        // TODO:
        // 1. Проверьте обязательные аргументы: id и moment не null, иначе IllegalArgumentException.
        // 2. Создайте документ нужного типа через фабричный метод newDocument(id, moment).
        // 3. Убедитесь, что подкласс вернул документ (не null) типа documentType()
        //    и в начальном статусе DRAFT; иначе IllegalStateException — это ошибка реализации фабрики.
        // 4. Верните документ.
        throw new UnsupportedOperationException("TODO");
    }

    /** Тип документов, которые создаёт фабрика. */
    public abstract DocumentsType documentType();

    /** Фабричный метод: создать экземпляр конкретного класса документа. */
    protected abstract ErpDocument newDocument(UUID id, LocalDateTime moment);

    /** Стратегия расчёта движений для этого типа документа. */
    public abstract StockMovementStrategy createMovementStrategy();

    /** Обработчик проведения этого типа документа. */
    public abstract AbstractDocumentProcessor createProcessor(ProcessingInfrastructure infrastructure);
}
