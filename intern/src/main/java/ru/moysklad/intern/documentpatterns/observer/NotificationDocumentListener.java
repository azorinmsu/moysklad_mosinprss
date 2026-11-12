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
        if (event.type().compareTo(DocumentEventType.DOCUMENT_CREATED) == 0)
            return;

        if (!notifiedEventIds.add(event.eventId()))
            return;

        String msg = String.format("Документ %s (%s): статус %s", event.documentType(), event.documentId(), event.type());
        if (event.errorCode() != null) {
            msg += ", ошибка: " + event.errorCode();
        }
        sender.send(msg);
    }
}
