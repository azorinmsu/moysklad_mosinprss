package ru.moysklad.intern.documentpatterns.processing;

import ru.moysklad.intern.documentpatterns.observer.DocumentEventPublisher;
import ru.moysklad.intern.documentpatterns.repository.StockRepository;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidator;

import java.util.Objects;

/**
 * Общие зависимости обработчиков проведения. Передаются в фабрику, которая создаёт обработчик своего типа.
 *
 * @param validationChain общая цепочка проверок (обычно {@code DocumentValidationChain.defaultChain()})
 * @param stockRepository хранилище остатков
 * @param eventPublisher  публикатор событий
 */
public record ProcessingInfrastructure(
        DocumentValidator validationChain,
        StockRepository stockRepository,
        DocumentEventPublisher eventPublisher
) {

    public ProcessingInfrastructure {
        Objects.requireNonNull(validationChain, "validationChain");
        Objects.requireNonNull(stockRepository, "stockRepository");
        Objects.requireNonNull(eventPublisher, "eventPublisher");
    }
}
