# Учебный проект: остатки, себестоимость и реализация

Один Spring Boot-модуль `intern/`: обработка складских остатков, себестоимости и заявок на реализацию товара поверх уже существующей модели серийных номеров.

**Заглушка Remap API 1.2:** [REMAP_STUB.md](REMAP_STUB.md).

**Задание «Обработка заказа покупателя» (Camunda 7.24 + МойСклад):** [ASSIGNMENT_CUSTOMER_ORDER.md](ASSIGNMENT_CUSTOMER_ORDER.md).

Ниже — отдельное задание по `AggregateStock` / Sales.

---

## Стек

| Компонент | Версия / выбор |
|---|---|
| Java | 27 |
| Spring Boot | 4.1.1 |
| БД | PostgreSQL + Liquibase |
| Messaging | Apache Kafka (Spring Kafka, Jackson 3) |
| Процессы | Camunda 7.24 embedded (`camunda-engine-spring`) |
| Тесты | JUnit 5, Testcontainers (PostgreSQL + Kafka), Awaitility |

Код приложения: `intern/src/main/java/ru/moysklad/intern/`.

Локальный запуск: Java 27, PostgreSQL, Kafka (`KAFKA_BOOTSTRAP_SERVERS`, по умолчанию `localhost:9092`), переменные из `.env` (см. `intern/.env.example`).

```bash
cd intern
./mvnw test
./mvnw spring-boot:run
```

---

## Архитектура

Система — **одно приложение**, которое можно запускать в нескольких инстансах. Разделение — по пакетам и ответственности, не по микросервисам.

### Две модели остатков

| Модель | Таблица / сущности | Назначение |
|---|---|---|
| Серийная (уже была) | `stock`, `SerialNumber`, `Lot`, `Warehouse`, документы | История и проверка серийников. Движок — `ThingStockManager` |
| Количественная (новая) | `aggregate_stock`, `price`, `stock_movement` | Агрегированное количество и себестоимость. Бизнес-логика шагов 2–3 — задание студента |

`Product` — каталожный товар. `Warehouse` — склад. `Lot` — партия (consignment). Существующий `Stock` **не** использовать как агрегированный количественный остаток.

### Потоки обработки

```text
[Клиент]
   │  POST /api/stock-change-requests
   ▼
StockChangeRequestService
   │  сохранить ProcessingRequest (PENDING)
   │  после commit → Kafka stock.commands
   ▼
StockChangeKafkaConsumer
   │  идемпотентность по requestId
   │  статус → PROCESSING
   ▼
Camunda process: stock-change
   │
   ├─ CHECK_SERIAL_NUMBERS      → ThingStockManager.checkThingStocks  (инфраструктура)
   ├─ CHECK_AVAILABLE_STOCK     → StockValidationStep                 (студент)
   ├─ UPDATE_STOCK_AND_PRICE    → StockAndPriceUpdateStep             (студент)
   ├─ PUBLISH_RESULT / FAILURE  → outbox processing.results           (инфраструктура)
   └─ END
```

```text
[Клиент]
   │  SalesRequestService.submit   (студент)
   ▼
SalesKafkaProducer → sales.commands   (студент)
   ▼
SalesKafkaConsumer                    (студент)
   │  сохранить заявку PENDING
   ▼
Camunda timer: sales-pending-timer    (инфраструктура процесса)
   │  ClaimPendingSalesDelegate
   │  → SalesRequestClaimStep         (студент: захват + старт)
   ▼
Camunda process: sales
   ├─ CHECK_SALES_STOCK           (студент)
   ├─ CHECK_AVAILABLE_AMOUNT      (студент)
   ├─ WRITE_OFF_STOCK             (студент)
   └─ PUBLISH_RESULT / FAILURE    (инфраструктура)
```

Kafka **не** читается из Camunda timer. Consumer пишет заявку в БД; timer обрабатывает уже сохранённые `PENDING`.

### Пакеты

```text
ru.moysklad.intern
├── entity/
│   ├── stock|operation|warehouse   # серийная модель и документы
│   ├── aggregate/                  # AggregateStock, Price, StockMovement
│   └── processing/                 # ProcessingRequest, ProcessingResult, OutboxEvent
├── stockchange/                    # приём StockChange + Kafka + запуск Camunda
├── sales/                          # каркасы Sales* — задание студента
├── camunda/
│   ├── StockChangeProcessDefinition / SalesProcessDefinition
│   ├── ProcessContext, ProcessErrors, ProcessOutcomeRecorder
│   ├── delegate/                   # JavaDelegate
│   └── step/                       # бизнес-шаги (часть — TODO)
├── kafka/                          # топики, типизированные KafkaTemplate
├── service/thing/                  # ThingStockManager (не менять checkThingStocks)
└── service/aggregate/              # PriceService
```

### Модель данных (количественная)

- **`aggregate_stock`** — `product_id`, `store_id`, `consignment_id`, `quantity` (`BigDecimal` ≥ 0), `version`; `UNIQUE(product_id, store_id, consignment_id)`.
- **`price`** — 1:1 к `aggregate_stock`; `unit_price`, `total_cost`; `totalCost = quantity × unitPrice`.
- **`stock_movement`** — история дельт для проверки документов задним числом. Текущий `AggregateStock.quantity` для исторической проверки **недостаточен**.
- **`processing_request`** — заявка (`requestId` unique, `commandType`, `status`, `correlationId`, `payload`, `version`).
- **`processing_result`** — итог по `requestId` (unique): `SUCCESS` / `ERROR` (+ код, сообщение, missing*).
- **`outbox_event`** — надёжная публикация результата в Kafka.

Миграция: `intern/src/main/resources/db/changelog/changes/004-aggregate-stock.yaml`.

### Kafka

| Topic | Кто пишет | Кто читает |
|---|---|---|
| `stock.commands` | `StockChangeKafkaProducer` (готово) | `StockChangeKafkaConsumer` (готово) |
| `sales.commands` | `SalesKafkaProducer` (**студент**) | `SalesKafkaConsumer` (**студент**) |
| `processing.results` | outbox → publisher (инфраструктура) | `ProcessingResult` consumer/API (инфраструктура) |

DTO: `StockChangeCommand`, `SalesCommand`, `ProcessingResultMessage`.

### Camunda

BPMN **не** редактируется вручную. Процессы строятся Java BPMN Model API и деплоятся при старте (`ProcessDeployment`).

Почему без starter: Camunda 7 Community Spring Boot Starter не совместим со Spring Boot 4. Движок подключён через `camunda-engine-spring` и `CamundaEngineConfig` (общий DataSource, таблицы `ACT_*`).

Бизнес-ошибка шага: `ProcessContext.fail(...)` → `BpmnError(BUSINESS_ERROR)` → `PUBLISH_FAILURE`.

### Существующая серийная модель (контекст)

`ThingStockManager` уже реализован, в том числе `checkThingStocks`. Студент **не** переписывает этот метод.

Типы документов: `GOODS_ACCEPTANCE`, `SHIPMENT`, `SHIFT`, `MACHINING`, `MAKE`.  
Склады в модели: `Warehouse` (`sourceStore` / `targetStore`). Партия: `Lot`.

Сигнатура:

```java
public void checkThingStocks(Collection<? extends DocumentsWithPositions> operations)
        throws ThingStockValidationException
```

Транзакция класса: `@Transactional(REQUIRES_NEW)`. Метод только проверяет, остатки серийников не меняет. Документы должны быть уже сохранены (`id != null`).

---

## Что уже сделано (не трогать без необходимости)

1. Liquibase-схема агрегированных остатков, цен, движений, заявок, outbox.
2. Репозитории и `PriceService`.
3. StockChange: REST → сервис → Kafka producer/consumer → запуск Camunda.
4. Программные определения процессов `stock-change`, `sales`, `sales-pending-timer`.
5. Delegates-обёртки, `ProcessContext`, запись результата в outbox.
6. Каркасы Sales* и шагов с TODO.
7. Тесты инфраструктуры: миграции, Kafka StockChange, деплой Camunda.

Не менять:

- алгоритм `ThingStockManager.checkThingStocks` / `checkThingStocksInternal`;
- готовый StockChange Kafka path;
- программные process definition (можно читать, менять порядок шагов только если это осознанно нужно заданию).

---

## Задание студенту

### Цель

Научитьcя атомарно менять остатки и себестоимость под конкурентной нагрузкой, корректно учитывать историю, провести заявку на реализацию через Kafka и Camunda.

### 1. Проверка количественных остатков — 15 баллов

Класс: `camunda.step.StockValidationStep#validate`.

Нужно:

- по позициям и типу документа определить дельты количества;
- учесть дату документа, склад, партию;
- проверить текущий и **исторический** остаток (`StockMovement`, не только `AggregateStock.quantity`);
- не допустить отрицательных остатков;
- при нехватке — `context.fail(...)` с понятным кодом/сообщением и `missingQuantity`.

### 2. Атомарное обновление AggregateStock и Price — 20 баллов

Класс: `camunda.step.StockAndPriceUpdateStep#execute`.

Нужно:

- изменить `AggregateStock` и `Price` в **одной** транзакции;
- пересчитать `totalCost`;
- при отсутствии `unitPrice` — бизнес-ошибка;
- запретить отрицательный остаток;
- при ошибке откатить оба изменения;
- на шаге 3 **повторно** обеспечить допустимость изменения (проверка на шаге 2 не гарантирует остаток к моменту записи).

### 3. Защита от lost update — 20 баллов

Приложение работает в нескольких потоках, consumers и инстансах. `synchronized` между JVM недостаточен.

Выберите стратегию (или комбинацию):

- `SELECT FOR UPDATE` / `FOR NO KEY UPDATE`;
- атомарный `UPDATE ... WHERE quantity >= ?`;
- optimistic locking (`version`) + retry;
- согласованный порядок блокировок;
- учёт конкурентного `INSERT` отсутствующего `AggregateStock`.

Обязательный сценарий: остаток 100, две параллельные списания по 70 → одна `SUCCESS`, вторая `INSUFFICIENT_STOCK`, итог quantity = 30, totalCost = 300.

### 4. SalesRequestService — 10 баллов

Класс: `sales.SalesRequestService`.

Приём заявки (товар, склад, количество, доступная сумма): валидация, `requestId`, сохранение, публикация через producer, возврат `requestId`.

### 5. Kafka producer и consumer для Sales — 15 баллов

Классы: `SalesKafkaProducer`, `SalesKafkaConsumer`.

- публикация в `sales.commands`;
- чтение, десериализация, идемпотентность по `requestId`;
- сохранение заявки со статусом `PENDING`;
- **не** читать Kafka из Camunda timer.

Бин `KafkaTemplate<String, SalesCommand>` уже объявлен в инфраструктуре.

### 6. Camunda-процесс реализации — 10 баллов

Шаги:

- `SalesStockCheckStep` — `INSUFFICIENT_STOCK` + `missingQuantity`;
- `SalesAmountCheckStep` — `requiredAmount = quantity × salePrice`, при нехватке `INSUFFICIENT_FUNDS` + `missingAmount`;
- `SalesWriteOffStep` — списание (желательно общая операция с AggregateStock/Price);
- `SalesRequestClaimStep` — захват `PENDING` так, чтобы одна заявка не стартовала дважды на разных инстансах.

### 7. Тесты и E2E — 10 баллов

Дописать заготовки (когда появятся в репозитории) и свой E2E реализации:

- успех;
- недостаточно товара **или** недостаточно денег;
- идемпотентность / отсутствие двойного списания.

Не использовать `Thread.sleep` как основной способ синхронизации (Awaitility / ожидание статусов).

### Критерии сдачи (обязательные)

| Условие |
|---|
| Нет lost update при параллельных списаниях |
| AggregateStock и Price меняются атомарно |
| Корректное списание при реализации |
| Работают Sales Kafka producer и consumer |
| Нет двойного списания при повторной доставке сообщения |
| Проходит E2E реализации |

### Баллы

| Задание | Баллы |
|---|---:|
| Проверка количественных остатков | 15 |
| Атомарное обновление AggregateStock и Price | 20 |
| Защита от lost update | 20 |
| SalesRequestService | 10 |
| Kafka producer и consumer | 15 |
| Camunda-процесс реализации | 10 |
| Самостоятельные тесты и E2E | 10 |
| **Всего** | **100** |

### Форматы

**StockChangeCommand** (уже используется инфраструктурой): `requestId`, `documentId`, `documentType`, `moment`, `positions[]` (`productId`, `storeId`, `destinationStoreId?`, `consignmentId`, `quantity`, `serialNumbers`, `unitPrice`), `correlationId`.

**SalesCommand**: `requestId`, `productId`, `storeId`, `quantity`, `availableAmount`, `requestedAt`, `correlationId`.

**Результат** (`ProcessingResultMessage`): `requestId`, `status` (`SUCCESS` / `ERROR`), `errorCode`, `errorMessage`, `missingQuantity`, `missingAmount`, `processedAt`.

API статуса заявки (инфраструктура): нет результата → `UNKNOWN` (это не то же самое, что `PENDING` у заявки).

### Подсказки по конкуренции (без готового SQL-решения)

Имеет смысл разобрать: lost update, race condition, optimistic / pessimistic locking, row-level locks, deadlock, уровни изоляции, идемпотентность, порядок захвата блокировок, retry при конфликте версии/транзакции.

---

## Предыдущее задание (выполнено, справочно)

Ранее нужно было реализовать `checkThingStocksInternal` для серийных номеров. Метод **уже реализован** в `ThingStockManager`. Ориентир по поведению — тесты `checkThingStocks*` в `ThingStockTest`. В новом задании этот код вызывается как готовый сервис на шаге `CHECK_SERIAL_NUMBERS`, а не переписывается.
