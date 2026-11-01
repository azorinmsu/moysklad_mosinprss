package ru.moysklad.intern.documentpatterns.processing;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Часть 4. Template Method — общий алгоритм проведения.
 */
class DocumentProcessorTest {

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Шаги выполняются в порядке: цепочка → специфичная проверка → расчёт → применение → событие")
    void stepsAreExecutedInOrder() {
        // TODO 1: Тестовый подкласс AbstractDocumentProcessor, который пишет имена шагов в общий список.
        // TODO 2: Цепочка — лямбда, пишущая "chain"; хранилище — обёртка, пишущая "apply";
        //  подписчик пишет "event:" + тип и статус документа.
        // TODO 3: Ожидаемый порядок и статус POSTED в момент события.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Успешное проведение приёмки: остаток вырос, статус POSTED, движения зафиксированы")
    void postsAcceptance() {
        // TODO 1: Приёмка 10 X на A; остаток A/X = 20.
        // TODO 2: new AcceptanceDocumentFactory().createProcessor(infra).process(document).
        // TODO 3: A/X = 30, статус POSTED, getPostedMovements() — одно движение +10, событие DOCUMENT_POSTED.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Ошибка валидации: статус DRAFT, остатки не изменились, событие ошибки без события проведения")
    void validationErrorKeepsDocumentDraft() {
        // TODO: отгрузка 100 при остатке 20 → INSUFFICIENT_STOCK; DRAFT; snapshot() прежний;
        //  событие DOCUMENT_PROCESSING_FAILED с кодом; нет DOCUMENT_POSTED.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Ошибка применения движений: статус DRAFT, нет события проведения")
    void applyFailureKeepsDocumentDraft() {
        // TODO: FailingStockRepository бросает DocumentProcessingException → тот же код наружу,
        //  статус DRAFT, getPostedMovements() пуст, нет DOCUMENT_POSTED.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Нет частичных изменений: перемещение при нехватке не меняет ни один склад")
    void noPartialChanges() {
        // TODO: цепочка-заглушка (d, c) -> {} пропускает документ, нехватку обнаруживает хранилище;
        //  остатки обоих складов прежние, history() пуст.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Повторное проведение → ALREADY_POSTED, остаток изменился один раз")
    void repeatedPostIsRejected() {
        // TODO: process дважды; второй раз код ALREADY_POSTED; остаток изменён один раз.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Специфичные правила типов: склад-источник у приёмки, дубли позиций в перемещении")
    void specificRules() {
        // TODO: приёмка с source → UNEXPECTED_SOURCE_WAREHOUSE; отгрузка с target → UNEXPECTED_TARGET_WAREHOUSE;
        //  перемещение с двумя позициями одного товара → DUPLICATE_POSITION.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("null-документ → DOCUMENT_REQUIRED; стратегия вызывается один раз за проведение")
    void nullDocumentAndSingleCalculation() {
        // TODO 1: process(null) → DOCUMENT_REQUIRED.
        // TODO 2: Считающая стратегия-обёртка + defaultChain(): после проведения счётчик = 1.
        fail("TODO: implement test");
    }
}
