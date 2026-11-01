package ru.moysklad.intern.documentpatterns.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.StockMovement;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.stock.Lot;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.lot;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.product;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.qty;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.warehouse;

/**
 * Инфраструктурные тесты готового in-memory хранилища. Проходят до выполнения заданий студентом.
 */
class InMemoryStockRepositoryTest {

    private final Product x = product("Товар X");
    private final Warehouse a = warehouse("Склад A");
    private final Warehouse b = warehouse("Склад B");
    private final UUID documentId = UUID.randomUUID();

    private StockMovement move(Warehouse warehouse, String delta) {
        return new StockMovement(documentId, x, warehouse, null, qty(delta));
    }

    @Test
    @DisplayName("Неизвестный ключ — нулевой остаток")
    void unknownKeyIsZero() {
        assertThat(new InMemoryStockRepository().getQuantity(x, a)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Положительная и отрицательная дельты меняют остаток")
    void appliesPositiveAndNegativeDeltas() {
        InMemoryStockRepository repository = new InMemoryStockRepository().setQuantity(x, a, qty("20"));

        repository.apply(List.of(move(a, "10")));
        repository.apply(List.of(move(a, "-7")));

        assertThat(repository.getQuantity(x, a)).isEqualByComparingTo("23");
    }

    @Test
    @DisplayName("Отрицательный остаток запрещён: INSUFFICIENT_STOCK, остаток и журнал не изменились")
    void negativeStockIsRejected() {
        InMemoryStockRepository repository = new InMemoryStockRepository().setQuantity(x, a, qty("5"));

        assertThatThrownBy(() -> repository.apply(List.of(move(a, "-6"))))
                .isInstanceOfSatisfying(InsufficientStockException.class, e -> {
                    assertThat(e.getCode()).isEqualTo(DocumentErrorCode.INSUFFICIENT_STOCK);
                    assertThat(e.getAvailable()).isEqualByComparingTo("5");
                    assertThat(e.getRequired()).isEqualByComparingTo("6");
                    assertThat(e.getMissing()).isEqualByComparingTo("1");
                });

        assertThat(repository.getQuantity(x, a)).isEqualByComparingTo("5");
        assertThat(repository.history()).isEmpty();
    }

    @Test
    @DisplayName("Набор движений атомарен: ошибка по одному ключу — не применено ни одно движение")
    void batchIsAtomic() {
        InMemoryStockRepository repository = new InMemoryStockRepository().setQuantity(x, a, qty("3"));
        Map<StockKey, BigDecimal> before = repository.snapshot();

        assertThatThrownBy(() -> repository.apply(List.of(move(b, "5"), move(a, "-5"))))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(repository.snapshot()).isEqualTo(before);
        assertThat(repository.getQuantity(x, b)).isEqualByComparingTo("0");
        assertThat(repository.history()).isEmpty();
    }

    @Test
    @DisplayName("Допустимость проверяется по суммарной дельте ключа в наборе")
    void checksNetDeltaPerKey() {
        InMemoryStockRepository repository = new InMemoryStockRepository().setQuantity(x, a, qty("2"));

        repository.apply(List.of(move(a, "-5"), move(a, "4")));

        assertThat(repository.getQuantity(x, a)).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("Журнал хранит каждое движение, номер записи и остаток после применения")
    void keepsHistory() {
        InMemoryStockRepository repository = new InMemoryStockRepository().setQuantity(x, a, qty("10"));
        UUID otherDocument = UUID.randomUUID();

        repository.apply(List.of(move(a, "-4"), move(b, "4")));
        repository.apply(List.of(new StockMovement(otherDocument, x, a, null, qty("1"))));

        List<StockHistoryEntry> history = repository.history();
        assertThat(history).hasSize(3);
        assertThat(history).extracting(StockHistoryEntry::sequence).containsExactly(1L, 2L, 3L);
        assertThat(history).extracting(StockHistoryEntry::quantityAfter)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(qty("6"), qty("4"), qty("7"));
        assertThat(repository.history(documentId)).hasSize(2);
        assertThat(repository.history(otherDocument)).hasSize(1);
    }

    @Test
    @DisplayName("Партия — часть ключа остатка")
    void lotIsPartOfKey() {
        Lot lot = lot("Партия 1");
        InMemoryStockRepository repository = new InMemoryStockRepository();

        repository.apply(List.of(new StockMovement(documentId, x, a, lot, qty("3"))));

        assertThat(repository.getQuantity(StockKey.of(x, a, lot))).isEqualByComparingTo("3");
        assertThat(repository.getQuantity(x, a)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Некорректные данные отклоняются")
    void rejectsInvalidInput() {
        InMemoryStockRepository repository = new InMemoryStockRepository();
        Product withoutId = new Product();
        withoutId.setName("Без id");

        assertThatThrownBy(() -> repository.apply(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.setQuantity(x, a, qty("-1"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.apply(List.of(new StockMovement(documentId, withoutId, a, null, qty("1")))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Снимок и журнал не позволяют изменить хранилище снаружи")
    void snapshotsAreImmutable() {
        InMemoryStockRepository repository = new InMemoryStockRepository().setQuantity(x, a, qty("1"));

        assertThatThrownBy(() -> repository.snapshot().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> repository.history().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Конкурентные списания: остаток 50, 100 потоков по 1 — успешно ровно 50, итог 0")
    void concurrentWriteOffsDoNotOversell() throws Exception {
        InMemoryStockRepository repository = new InMemoryStockRepository().setQuantity(x, a, qty("50"));
        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    try {
                        repository.apply(List.of(move(a, "-1")));
                        return true;
                    } catch (InsufficientStockException e) {
                        return false;
                    }
                }));
            }
            start.countDown();
            int succeeded = 0;
            for (Future<Boolean> result : results) {
                if (result.get(10, TimeUnit.SECONDS)) {
                    succeeded++;
                }
            }
            assertThat(succeeded).isEqualTo(50);
        } finally {
            pool.shutdownNow();
        }
        assertThat(repository.getQuantity(x, a)).isEqualByComparingTo("0");
        assertThat(repository.history()).hasSize(50);
    }
}
