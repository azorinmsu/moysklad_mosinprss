package ru.moysklad.intern.documentpatterns.observer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Часть 8. Observer — события документов.
 */
class DocumentObserverTest {

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Публикация доставляет событие подписчику")
    void publishDeliversEvent() {
        // TODO: subscribe(RecordingEventListener); publish(event) → получено ровно это событие.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Несколько подписчиков в порядке подписки; повторная подписка не дублирует; отписка")
    void multipleSubscribers() {
        // TODO: два подписчика получают событие; повторный subscribe того же — одна доставка;
        //  после unsubscribe события не приходят.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Неуспешное проведение не публикует DOCUMENT_POSTED")
    void noPostedEventOnFailure() {
        // TODO: отгрузка при нехватке → только DOCUMENT_PROCESSING_FAILED с кодом INSUFFICIENT_STOCK.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Ошибка подписчика не ломает проведение и доставку остальным")
    void listenerFailureDoesNotBreakPosting() {
        // TODO: первый подписчик бросает исключение; документ POSTED; второй подписчик получил событие;
        //  deliveryFailures() содержит одну запись.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Повторная обработка той же операции не создаёт повторного уведомления")
    void noDuplicateNotifications() {
        // TODO 1: executor.execute(command) дважды → одно уведомление о проведении.
        // TODO 2: повторная доставка того же события NotificationDocumentListener/AuditDocumentListener → без дублей.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Корректные типы событий и уведомления по ним")
    void eventTypesAndNotifications() {
        // TODO: проведение → DOCUMENT_POSTED (статус POSTED), отмена → DOCUMENT_UNPOSTED (CANCELLED);
        //  NotificationDocumentListener не уведомляет о DOCUMENT_CREATED, уведомляет об остальных;
        //  текст ошибки содержит код.
        fail("TODO: implement test");
    }
}
