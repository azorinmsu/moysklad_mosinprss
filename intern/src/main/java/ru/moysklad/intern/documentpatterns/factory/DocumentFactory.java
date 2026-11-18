package ru.moysklad.intern.documentpatterns.factory;

import ru.moysklad.intern.documentpatterns.model.DocumentStatus;
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
        if (id == null)
            throw new IllegalArgumentException("ID не может быть нулевым");
        if (moment == null)
            throw new IllegalArgumentException("Дата не может быть нулевой");

        ErpDocument doc = newDocument(id, moment);

        // проверки на null
        if (doc == null)
            // я вот не знаю, как еще сообщения об ошибке писать?
            throw new IllegalStateException("Ошибка создания документа - нулевой документ");
        if (doc.getType() != documentType() || doc.getType() == null)
            throw new IllegalStateException("Ошибка создания документа - отсутствует валидный тип документа");
        if (doc.getStatus() != DocumentStatus.DRAFT)
            throw new IllegalStateException("Ошибка создания документа - документ не является черновиком");

        return doc;
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
