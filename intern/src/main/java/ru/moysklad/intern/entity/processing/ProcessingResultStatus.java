package ru.moysklad.intern.entity.processing;

/**
 * Статус сохранённого результата обработки.
 * API-статус {@code UNKNOWN} возвращается сервисом, когда записи ещё нет.
 */
public enum ProcessingResultStatus {
    SUCCESS,
    ERROR
}
