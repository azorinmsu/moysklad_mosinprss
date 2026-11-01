package ru.moysklad.intern.documentpatterns.decorator;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Часть 5. Decorator — дополнительные возможности обработки.
 * Для изоляции подставляйте базовую операцию-лямбду, например {@code doc -> doc.applyStatus(POSTED)}.
 */
class DocumentDecoratorTest {

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Декоратор выполняет базовую операцию ровно один раз")
    void delegatesToBaseOperation() {
        // TODO: счётчик в базовой лямбде == 1 после LoggingDecorator и AuditDecorator.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Логирование: начало и успех, результат операции не меняется")
    void loggingWritesStartAndSuccess() {
        // TODO: две строки лога, вторая содержит итоговый статус POSTED; документ POSTED.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Аудит успешной операции: SUCCESS, статус DRAFT → POSTED")
    void auditRecordsSuccess() {
        // TODO: одна запись AuditRecord с operation, statusBefore/After, outcome SUCCESS, errorCode null.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Комбинация декораторов в любом порядке: одна запись аудита и строки лога")
    void decoratorsCombine() {
        // TODO: Audit(Logging(base)) и Logging(Audit(base)).
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Ошибка основной операции пробрасывается тем же исключением, в логе — ошибка")
    void failureIsPropagated() {
        // TODO: базовая лямбда бросает DocumentProcessingException → тот же экземпляр (isSameAs), лог содержит код.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Аудит ошибки: FAILURE с кодом, статус не стал POSTED")
    void auditRecordsFailure() {
        // TODO: реальный обработчик отгрузки при нехватке → FAILURE, INSUFFICIENT_STOCK, statusAfter DRAFT.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test (дополнительное задание)")
    @DisplayName("Дополнительно: TimingDecorator измеряет длительность и при успехе, и при ошибке")
    void timingDecoratorMeasuresDuration() {
        // TODO: фиктивный nanoTime (100, 350) → Duration.ofNanos(250); при ошибке длительность тоже передана.
        fail("TODO: implement test");
    }
}
