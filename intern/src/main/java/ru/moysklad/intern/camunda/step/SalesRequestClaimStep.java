package ru.moysklad.intern.camunda.step;

import org.springframework.stereotype.Component;

/**
 * Выбор и захват PENDING-заявок реализации. Вызывается timer-процессом, не Kafka consumer.
 *
 * <p><b>TODO (студент):</b> забрать заявку так, чтобы при нескольких инстансах она не стартовала дважды.
 * Нужны идемпотентность, защита от повторной обработки и восстановление после падения.
 * Здесь намеренно пустая реализация: timer не должен падать, пока шаг не написан.
 */
@Component
public class SalesRequestClaimStep {

    public void claimAndStart() {
        // TODO(student): выбрать PENDING-заявки, захватить одну и запустить процесс sales.
    }
}
