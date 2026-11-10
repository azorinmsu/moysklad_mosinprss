package ru.moysklad.intern.documentpatterns.decorator;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;

import java.time.Duration;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;

/**
 * Дополнительное задание: измеряет длительность операции.
 *
 * <p>Источник времени ({@code nanoTime}) передаётся снаружи, чтобы тест был детерминированным:
 * в проде — {@code System::nanoTime}, в тесте — счётчик с заданными значениями.
 */
public class TimingDecorator extends DocumentOperationDecorator {

    private final LongSupplier nanoTime;
    private final BiConsumer<ErpDocument, Duration> sink;

    public TimingDecorator(DocumentOperation delegate, LongSupplier nanoTime, BiConsumer<ErpDocument, Duration> sink) {
        super(delegate);
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    @Override
    public void execute(ErpDocument document) {
        long start = nanoTime.getAsLong();
        try {
            delegate.execute(document);
        } finally {
            long result = nanoTime.getAsLong() - start;
            sink.accept(document, Duration.ofNanos(result));
        }
    }
}
