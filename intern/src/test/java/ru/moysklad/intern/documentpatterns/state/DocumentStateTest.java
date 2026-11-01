package ru.moysklad.intern.documentpatterns.state;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Часть 7. State — жизненный цикл документа.
 */
class DocumentStateTest {

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("DRAFT → POSTED: переход запланирован, применяется только в commit()")
    void draftToPosted() {
        // TODO: context.post() → plannedStatus POSTED, документ ещё DRAFT; commit() → POSTED.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("POSTED → CANCELLED")
    void postedToCancelled() {
        // TODO: документ в POSTED; context.unpost(); commit() → CANCELLED.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Повторное проведение → ALREADY_POSTED, переход не запланирован")
    void repeatedPostIsForbidden() {
        // TODO: POSTED + post() → DocumentStateException ALREADY_POSTED; plannedStatus() пуст.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Отмена черновика → NOT_POSTED")
    void unpostDraftIsForbidden() {
        // TODO: DRAFT + unpost() → NOT_POSTED.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("CANCELLED — конечное состояние: повторная отмена и проведение → ALREADY_CANCELLED")
    void cancelledIsTerminal() {
        // TODO: CANCELLED + unpost() и post() → ALREADY_CANCELLED.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Ошибка выполнения операции сохраняет предыдущий статус")
    void failureKeepsPreviousStatus() {
        // TODO 1: Обработчик отгрузки при нехватке → документ остаётся DRAFT.
        // TODO 2: context.post() без commit() (операция «упала») → статус DRAFT.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Каждое состояние соответствует своему статусу")
    void statesMatchStatuses() {
        // TODO: DraftState.INSTANCE.status() == DRAFT и т.д.
        fail("TODO: implement test");
    }
}
