package ru.moysklad.intern.documentpatterns.decorator;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Логирует начало, успех и ошибку операции. Бизнес-результат не меняет.
 */
public class LoggingDecorator extends DocumentOperationDecorator {

    private final Consumer<String> log;

    public LoggingDecorator(DocumentOperation delegate, Consumer<String> log) {
        super(delegate);
        this.log = Objects.requireNonNull(log, "log");
    }

    @Override
    public void execute(ErpDocument document) {
        // TODO:
        // 1. Запишите в log строку о начале: тип и id документа, текущий статус.
        // 2. Вызовите delegate.execute(document).
        // 3. При успехе запишите строку об успехе с итоговым статусом.
        // 4. При RuntimeException запишите строку об ошибке (код DocumentProcessingException, если есть)
        //    и пробросьте ТО ЖЕ исключение дальше — логирование не должно «глотать» ошибки.
        throw new UnsupportedOperationException("TODO");
    }
}
