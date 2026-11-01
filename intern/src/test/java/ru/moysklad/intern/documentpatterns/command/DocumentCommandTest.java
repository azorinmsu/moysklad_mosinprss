package ru.moysklad.intern.documentpatterns.command;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Часть 6. Command — проведение и отмена проведения.
 */
class DocumentCommandTest {

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Проведение командой: EXECUTED, остаток изменился, запись в истории")
    void postCommand() {
        // TODO 1: PostDocumentCommand(operationId, приёмка, обработчик) через DocumentCommandExecutor.
        // TODO 2: Результат EXECUTED, остаток вырос, history() — одна запись EXECUTED.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Отмена проведения: обратные движения восстанавливают остатки, статус CANCELLED")
    void unpostCommand() {
        // TODO: провести перемещение A→B 5, затем UnpostDocumentCommand → A и B как до проведения, CANCELLED.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Повторное выполнение той же команды пропускается, новая команда проведения → ALREADY_POSTED")
    void repeatedPost() {
        // TODO 1: executor.execute(command) дважды → EXECUTED, SKIPPED_DUPLICATE; остаток изменён один раз.
        // TODO 2: Новая PostDocumentCommand для проведённого документа → ALREADY_POSTED, в истории FAILED.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Повторная отмена → ALREADY_CANCELLED, остатки не меняются")
    void repeatedUnpost() {
        // TODO: вторая UnpostDocumentCommand с новым operationId → ALREADY_CANCELLED; snapshot() прежний.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Отмена черновика → NOT_POSTED")
    void unpostDraftIsRejected() {
        // TODO: UnpostDocumentCommand для черновика → NOT_POSTED, статус DRAFT.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Отмена при недостаточном остатке: товар уже ушёл со склада-получателя")
    void unpostWithInsufficientStock() {
        // TODO 1: Перемещение 5 X A→B проведено, затем отгрузка 5 X с B проведена.
        // TODO 2: Отмена перемещения → INSUFFICIENT_STOCK; статус POSTED; остатки не изменились.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("История: журнал остатков хранит прямые и обратные движения, исполнитель — операции")
    void historyIsPreserved() {
        // TODO: stocks.history(transferId) — 4 записи (-5, +5, +5, -5);
        //  executor.history() — POST и UNPOST с EXECUTED.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Одна операция из нескольких потоков выполняется ровно один раз")
    void concurrentDuplicateIsExecutedOnce() {
        // TODO: 8 потоков выполняют одну и ту же команду → один EXECUTED, остаток изменён один раз.
        fail("TODO: implement test");
    }
}
