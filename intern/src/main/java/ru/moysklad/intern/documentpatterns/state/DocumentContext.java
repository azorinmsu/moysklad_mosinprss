package ru.moysklad.intern.documentpatterns.state;

import ru.moysklad.intern.documentpatterns.model.DocumentStatus;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;

import java.util.Objects;
import java.util.Optional;

/**
 * Контекст паттерна State для одной операции над документом. <b>Готовая инфраструктура.</b>
 *
 * <p>Двухфазная смена статуса:
 * <ol>
 *     <li>{@link #post()} / {@link #unpost()} — текущее состояние проверяет допустимость и вызывает
 *     {@link #transitionTo(DocumentStatus)}: переход только <i>запланирован</i>, документ не меняется;</li>
 *     <li>операция выполняет работу (валидация, движения);</li>
 *     <li>{@link #commit()} — применяет запланированный статус к документу.</li>
 * </ol>
 * Если на шаге 2 возникла ошибка, {@code commit()} не вызывается и документ сохраняет прежний статус.
 */
public final class DocumentContext {

    private final ErpDocument document;
    private final DocumentStatus initialStatus;
    private DocumentStatus plannedStatus;

    public DocumentContext(ErpDocument document) {
        this.document = Objects.requireNonNull(document, "document");
        this.initialStatus = document.getStatus();
    }

    public ErpDocument document() {
        return document;
    }

    /** Единственное место, где статус документа сопоставляется объекту-состоянию. */
    public DocumentState currentState() {
        return switch (document.getStatus()) {
            case DRAFT -> DraftState.INSTANCE;
            case POSTED -> PostedState.INSTANCE;
            case CANCELLED -> CancelledState.INSTANCE;
        };
    }

    public void post() {
        currentState().post(this);
    }

    public void unpost() {
        currentState().unpost(this);
    }

    /** Вызывается состоянием: планирует переход в {@code target}. Документ пока не меняется. */
    public void transitionTo(DocumentStatus target) {
        Objects.requireNonNull(target, "target");
        if (plannedStatus != null) {
            throw new IllegalStateException("Переход уже запланирован: " + initialStatus + " -> " + plannedStatus);
        }
        plannedStatus = target;
    }

    public Optional<DocumentStatus> plannedStatus() {
        return Optional.ofNullable(plannedStatus);
    }

    /** Применяет запланированный переход. Вызывайте только после успешного выполнения операции. */
    public void commit() {
        if (plannedStatus == null) {
            throw new IllegalStateException("Нет запланированного перехода для документа " + document.getId());
        }
        if (document.getStatus() != initialStatus) {
            throw new IllegalStateException("Статус документа " + document.getId() + " изменился во время операции: "
                    + initialStatus + " -> " + document.getStatus());
        }
        document.applyStatus(plannedStatus);
        plannedStatus = null;
    }
}
