package ru.moysklad.concurrency.domain;

import java.util.List;

public record WarehouseReport(
        List<Integer> balances,
        List<String> failedWarehouses,
        boolean completed
) {
}
