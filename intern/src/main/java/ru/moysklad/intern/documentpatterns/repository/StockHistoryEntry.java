package ru.moysklad.intern.documentpatterns.repository;

import ru.moysklad.intern.documentpatterns.model.StockMovement;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Запись журнала движений: какое движение применено и каким стал остаток.
 *
 * @param sequence      порядковый номер записи в репозитории (монотонно растёт)
 * @param movement      применённое движение
 * @param quantityAfter остаток по ключу движения после применения
 * @param appliedAt     момент применения
 */
public record StockHistoryEntry(long sequence, StockMovement movement, BigDecimal quantityAfter, Instant appliedAt) {
}
