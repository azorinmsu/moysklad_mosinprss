package ru.moysklad.intern.documentpatterns;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Часть 10. Сквозной сценарий через {@code DocumentService} без внешних сервисов.
 *
 * <pre>
 * Начальные остатки (TestStocks.workflowInitialStocks): A: X=20, Y=10; B: X=0, Y=0
 * 1. Приёмка 10 X на A           → A/X = 30
 * 2. Отгрузка 7 X с A            → A/X = 23
 * 3. Перемещение 5 X с A на B    → A/X = 18, B/X = 5
 * 4. Отмена перемещения          → A/X = 23, B/X = 0
 * 5. Отгрузка 100 X с A          → INSUFFICIENT_STOCK, остатки прежние, DRAFT, нет DOCUMENT_POSTED
 * </pre>
 */
class DocumentWorkflowTest {

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Сквозной сценарий: приёмка → отгрузка → перемещение → отмена → ошибка")
    void fullDocumentLifecycle() {
        // TODO 1: DocumentService.withDefaultDocuments(stocks, publisher, auditTrail, log::add),
        //  подписать RecordingEventListener.
        // TODO 2: Шаги 1–4 через service.create(...) и service.post(...)/unpost(...), после каждого — остатки.
        // TODO 3: Шаг 5: INSUFFICIENT_STOCK, snapshot() прежний, статус DRAFT,
        //  для этого документа нет DOCUMENT_POSTED, есть DOCUMENT_PROCESSING_FAILED.
        // TODO 4: Остаток Y не менялся на протяжении всего сценария.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Нет двойного изменения остатков при повторе операции с тем же operationId")
    void noDoubleStockChange() {
        // TODO: service.post(doc, opId) дважды → EXECUTED, SKIPPED_DUPLICATE; остаток изменён один раз;
        //  одно событие DOCUMENT_POSTED.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Неподдерживаемый тип документа (MAKE) → UNSUPPORTED_DOCUMENT_TYPE")
    void unsupportedDocumentType() {
        // TODO: service.create(DocumentsType.MAKE, b -> {}) → UNSUPPORTED_DOCUMENT_TYPE.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Аудит, история операций и события отражают сценарий")
    void auditHistoryAndEvents() {
        // TODO: после проведения и отмены: события CREATED, POSTED, UNPOSTED по порядку;
        //  auditTrail — запись POST SUCCESS; operationHistory() — POST и UNPOST; лог не пуст.
        fail("TODO: implement test");
    }
}
