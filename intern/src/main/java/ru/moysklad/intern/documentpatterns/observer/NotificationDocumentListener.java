package ru.moysklad.intern.documentpatterns.observer;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Отправляет уведомления о проведении, отмене проведения и ошибках обработки.
 * Обработчики документов про этот класс не знают — они только публикуют события.
 */
public class NotificationDocumentListener implements DocumentEventListener {

    private final NotificationSender sender;
    private final Set<UUID> notifiedEventIds = ConcurrentHashMap.newKeySet();

    public NotificationDocumentListener(NotificationSender sender) {
        this.sender = Objects.requireNonNull(sender, "sender");
    }

    @Override
    public void onEvent(DocumentEvent event) {
        // TODO:
        // 1. Уведомляйте о DOCUMENT_POSTED, DOCUMENT_UNPOSTED и DOCUMENT_PROCESSING_FAILED;
        //    DOCUMENT_CREATED уведомления не требует.
        // 2. Не отправляйте уведомление повторно для того же eventId (notifiedEventIds.add(...) атомарен).
        // 3. Текст сообщения должен содержать тип документа, его id и (для ошибки) код ошибки.
        // 4. Отправьте сообщение через sender.
        throw new UnsupportedOperationException("TODO");
    }
}
