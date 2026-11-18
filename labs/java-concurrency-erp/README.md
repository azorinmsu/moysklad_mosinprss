# Многопоточность Java: учёт остатков ERP

Самостоятельное Spring Boot-приложение для лабораторной магистратуры.
Тема: обнаружение и исправление ошибок конкурентного доступа и реализация собственных синхронизаторов.
Предметная область — упрощённый контур МойСклад: остатки, резервы, заказы, отгрузки, закупки, перемещения, цены, отчёты и фоновые задачи.

Модуль `intern/` этого репозитория не используется и не изменяется. Внешние API МойСклад, PostgreSQL и Kafka не нужны.

## Стек

| Компонент | Версия |
|---|---|
| Java | 21 (bytecode `--release 21`) |
| Spring Boot | 3.5.7 |
| Сборка | Maven |
| Тесты | JUnit 5, AssertJ |

Модуль `intern` собран на Java 27 и Spring Boot 4. Лабораторная сознательно остаётся на Java 21 и Spring Boot 3: это отдельное приложение, и такие версии заданы учебным заданием. Проверено, что те же классы запускаются и на JDK 27, если компилятор выпускает bytecode 21.

## Запуск

Нужен JDK 21 или новее.

```bash
cd labs/java-concurrency-erp
./mvnw test
./mvnw spring-boot:run
```

Приложение поднимается на `http://localhost:8080`.

| Метод | Путь | Действие |
|---|---|---|
| GET | `/api/health` | проверка процесса |
| GET | `/api/reports/stock` | остатки |
| POST | `/api/stocks` | начальный остаток |
| POST | `/api/reservations` | резерв |
| POST | `/api/sales-orders` | заказ покупателя |
| POST | `/api/shipments` | отгрузка |
| POST | `/api/purchase-orders` | приход по закупке |
| POST | `/api/transfers` | перемещение |
| PUT | `/api/prices/{productId}` | цена |
| POST | `/api/movements` | движение остатка |
| POST | `/api/order-events` | событие заказа в контексте организации |

При старте создаются остатки `paper-a4` и `pen-blue`.

## Тесты

`./mvnw test` **обязан падать**, пока ошибки не исправлены и синхронизаторы не реализованы. Это исходное состояние лабораторной, а не поломка сборки.

Группы JUnit 5:

```bash
./mvnw test -Dgroups=baseline
./mvnw test -Dgroups=bug
./mvnw test -Dgroups=synchronizer
./mvnw -Dtest=Bug01Test test
./mvnw -Dtest=CustomSemaphoreTest test
```

| Группа | Что проверяет | В исходном коде |
|---|---|---|
| `baseline` | однопоточные инварианты и запуск Spring | проходит |
| `bug` | 15 конкурентных дефектов | падает |
| `synchronizer` | четыре собственных синхронизатора | почти все тесты падают: заготовки бросают `UnsupportedOperationException`. Проверки конструктора на отрицательные аргументы уже выполнены |

Задание для студента: [STUDENT_TASKS.md](STUDENT_TASKS.md).

## Структура

```text
labs/java-concurrency-erp/
  pom.xml
  README.md
  STUDENT_TASKS.md
  src/main/java/ru/moysklad/concurrency/
    ConcurrencyErpApplication.java
    domain/          Product, Warehouse, StockBalance, Reservation, SalesOrder,
                     Shipment, PurchaseOrder, StockTransfer, Price, StockMovement
    inventory/       StockLedger, InventoryService
    reservation/     ReservationService
    orders/          SalesOrderService, PurchaseOrderService
    shipment/        ShipmentService
    transfer/        StockTransferService
    pricing/         PriceService
    reports/         ReportService
    events/          EventProcessingService
    jobs/            ReportRefreshTask, ExternalSyncGateway, OrderEventProcessor
    synchronizers/   заготовки SYNC-01 … SYNC-04
    web/             REST
    support/         SyncPoint
  src/test/java/ru/moysklad/concurrency/
    bugs/            Bug01Test … Bug15Test, ErpBaselineTest
    synchronizers/   тесты четырёх синхронизаторов
    support/         ConcurrencyTestProbe, ConcurrentTasks
    web/             ErpApplicationTest
```

Конкурентные тесты вызывают сервисы напрямую, без поднятия Spring-контекста. Потоки настоящие. `SyncPoint` в рабочем режиме ничего не блокирует; тесты подставляют `ConcurrencyTestProbe`, чтобы воспроизведение не зависело от планировщика.

Вызовы `SyncPoint` в сервисах — часть оснастки. Их нужно сохранить в тех же логических точках, иначе детерминированный тест перестанет управлять расписанием.
