package ru.moysklad.intern.entity.processing;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
