package ru.moysklad.intern.documentpatterns.strategy;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.StockMovement;

import java.util.List;

/**
 * Паттерн Strategy: правило расчёта складских движений для типа документа.
 *
 * <p>Стратегия только <b>вычисляет</b> движения и не имеет доступа к хранилищу остатков:
 * расчёт отделён от выполнения, его легко тестировать и переиспользовать (проверка остатков,
 * предпросмотр, отмена проведения).
 */
@FunctionalInterface
public interface StockMovementStrategy {

    List<StockMovement> calculateMovements(ErpDocument document);
}
