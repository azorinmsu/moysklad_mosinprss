# Заглушка Remap API 1.2

Stateful HTTP-заглушка подмножества Remap API для учебного задания по обработке заказа покупателя (Camunda 7.24 + Java SDK МойСклад). Живёт внутри приложения `intern`, состояние хранит в PostgreSQL.

Код: `intern/src/main/java/ru/moysklad/intern/remapstub/`.  
Задание: [ASSIGNMENT_CUSTOMER_ORDER.md](ASSIGNMENT_CUSTOMER_ORDER.md).

---

## Включение

В `application.properties`:

```properties
app.remap-stub.enabled=true
app.remap-stub.seed-on-startup=true
app.remap-stub.public-base-url=http://localhost:8080
```

После старта (`./mvnw spring-boot:run`) заглушка на том же порту, что и приложение.

```java
ApiClient api = new ApiClient("localhost:8080", false, "student@company", "password");
```

SDK добавляет префикс `/api/remap/1.2`. Зависимость: `api-remap-1.2-sdk:9.2`.

---

## Методы заглушки в контексте задания

Ниже — **что вызывать на каком этапе** процесса. HTTP — контракт Remap; колонка SDK — типичный вызов `api-remap-1.2-sdk`.

### Этап 1 — получение и проверка заказа

| HTTP | SDK | Что делает в задании |
|---|---|---|
| `GET /entity/customerorder/{id}?expand=positions` | `api.entity().customerorder().get(id, ExpandParam.expand("positions"))` | Загрузить заказ и позиции: существует ли заказ, есть ли позиции, какие `quantity` / assortment. Базовая проверка перед стартом процесса. |
| `GET /entity/customerorder/{id}/positions` | `api.entity().customerorder().getPositions(id)` | Альтернатива: позиции отдельным запросом, если заказ без `expand`. |
| `GET /entity/customerorder/{id}/positions/{positionId}` | `api.entity().customerorder().getPosition(id, positionId)` | Одна позиция (точечная проверка). |
| `GET /entity/product/{id}` | `api.entity().product().get(id)` | Убедиться, что товар из позиции существует / не архивен (опционально к заказу). |

### Этап 2 — проверка остатков

| HTTP | SDK | Что делает в задании |
|---|---|---|
| `GET /report/stock/all` | **нет в SDK** — `RestClient` / `WebClient` / HTTP | Главный способ: для каждого товара из заказа взять `quantity` (**available** = stock − reserve − inTransit), сравнить с нужным количеством. Учитывает уже существующие резервы. |
| `GET .../customerorder/{id}?fields=stock&expand=positions` | `fields` **не** в `ApiParam` SDK | В JSON позиций может быть блок `stock`; удобнее для сырого HTTP. Для typed SDK надёжнее `report/stock/all`. |

При нехватке остатков процесс ждёт (таймер BPMN) и **снова** вызывает эти же методы — заглушка отражает пополнение, если вы изменили данные / сбросили сид / другой процесс снял резерв.

### Этап 3 — резервирование (и снятие резерва при компенсации)

| HTTP | SDK | Что делает в задании |
|---|---|---|
| `PUT /entity/customerorder/{id}/positions/{positionId}` тело `{"reserve": N}` | `CustomerOrderDocumentPosition` + `updatePosition(orderId, positionId, pos)` | **Резервирование:** выставить `reserve` по позиции. Заглушка увеличивает складской `reserve`, уменьшает available. Повтор с тем же `N` — идемпотентен на стороне API (абсолютное значение, не «добавить ещё N»); идемпотентность процесса (не вызывать лишний раз / хранить факт) — задача студента. |
| `PUT /entity/customerorder/{id}` с `positions.rows[].reserve` | `update(order)` с позициями | Массовое изменение резервов по нескольким позициям одного заказа. |
| Тот же `PUT` с `"reserve": 0` | то же | **Компенсация / отмена:** освободить резерв. Available снова растёт. |

Отдельных URL `/reserve` / `/releaseReserve` **нет** — только поле `reserve`, как в реальном Remap.

### Этап 4 — ожидание подтверждения отгрузки

К заглушке Remap на этом этапе обращений обычно **нет**: сигнал «подтвердить / отклонить» идёт в **ваш** REST API приложения (message correlate в Camunda).  
При **отклонении** студент вызывает компенсацию этапа 3 (снятие `reserve`) через методы выше.

### Этап 5 — создание отгрузки

| HTTP | SDK | Что делает в задании |
|---|---|---|
| `POST /entity/demand` | `api.entity().demand().create(demand)` | Создать документ отгрузки, связать с заказом (`customerOrder`), передать organization / agent / store / positions. По умолчанию черновик: `applicable=false` (остаток на складе ещё не списывается). |
| | | Заглушка **не** дедуплицирует: два успешных POST → два demand. Сценарий `LOSE_RESPONSE` + retry Camunda проверяет идемпотентность **вашего** кода. |

### Этап 6 — проверка отгрузки

| HTTP | SDK | Что делает в задании |
|---|---|---|
| `GET /entity/demand/{id}?expand=positions` | `api.entity().demand().get(id, ExpandParam.expand("positions"))` | Прочитать созданную отгрузку: `applicable`, связь с заказом, состав. |
| `GET /entity/demand/{id}/positions` | `api.entity().demand().getPositions(id)` | Сверить товары и количества с заказом. |

### Этап 7 — подтверждение (проведение) отгрузки — точка необратимости

| HTTP | SDK | Что делает в задании |
|---|---|---|
| `PUT /entity/demand/{id}` тело `{"applicable": true}` | `Demand` + `update(id, demand)` | **Проведение:** списывает `quantity` со склада, снимает связанный резерв по заказу. После успеха автоматическая отмена процесса в задании **запрещена**. Повторный PUT с `applicable=true` для уже проведённой — без повторного списания (уже проведена). |
| `PUT` с `"applicable": false` | то же | Снять проведение (вернуть остаток/резерв) — для учебных компенсаций **до** точки необратимости, если вы так спроектировали; после успешного этапа 7 в задании автоотмена не делается. |

### Этап 8 — завершение

К заглушке обычно не нужно: статус `COMPLETED` и id документов храните в своём API / переменных Camunda. При необходимости финально перечитайте заказ/отгрузку методами GET выше.

### Компенсация / отмена (после создания черновика demand)

| HTTP | SDK | Что делает в задании |
|---|---|---|
| `DELETE /entity/demand/{id}` | `api.entity().demand().delete(id)` | Удалить **черновик** (`applicable=false`). Проведённую отгрузку так удалить нельзя (заглушка вернёт конфликт) — сначала снимите проведение или не допускайте отмену после этапа 7. |
| `PUT .../positions/...` `"reserve": 0` | `updatePosition` | Освободить резерв заказа после отмены / отклонения. |

---

## Сводная таблица endpoints

| Метод | Путь | Этап(ы) задания |
|---|---|---|
| GET | `/api/remap/1.2/entity/product/{id}` | 1 |
| GET | `/api/remap/1.2/entity/customerorder/{id}` | 1, 2 |
| PUT | `/api/remap/1.2/entity/customerorder/{id}` | 3, компенсация |
| GET | `/api/remap/1.2/entity/customerorder/{id}/positions` | 1, 2 |
| GET | `/api/remap/1.2/entity/customerorder/{id}/positions/{positionId}` | 1, 3 |
| PUT | `/api/remap/1.2/entity/customerorder/{id}/positions/{positionId}` | 3, компенсация |
| GET | `/api/remap/1.2/report/stock/all` | 2 |
| POST | `/api/remap/1.2/entity/demand` | 5 |
| GET | `/api/remap/1.2/entity/demand/{id}` | 6, 8 |
| PUT | `/api/remap/1.2/entity/demand/{id}` | 7 (и опционально откат проведения) |
| DELETE | `/api/remap/1.2/entity/demand/{id}` | компенсация черновика |
| GET | `/api/remap/1.2/entity/demand/{id}/positions` | 6 |

---

## Технический API заглушки (не Remap)

Для тестов студента и настройки сбоев — не часть Remap 1.2.

| HTTP | Класс / метод | В контексте задания |
|---|---|---|
| `POST /api/stub/reset` | `RemapStubAdminController.reset` → `RemapStubSeedService.resetToSeed` | Перед сценарием теста вернуть сиды и очистить fault-правила. |
| `GET /api/stub/faults` | `listFaults` | Посмотреть активные правила эмуляции. |
| `POST /api/stub/faults` | `addFault` | Включить сбой на следующий(ие) Remap-запрос(ы): 429/500/503, delay, timeout, обрыв, **потеря ответа после успеха**. |
| `DELETE /api/stub/faults` | `clearFaults` | Сбросить эмуляцию сбоев. |

Пример: следующий `POST /entity/demand` выполнится в БД, но клиент не получит ответ (проверка идемпотентности этапа 5):

```json
{
  "method": "POST",
  "pathPattern": "/api/remap/1.2/entity/demand",
  "faultType": "LOSE_RESPONSE",
  "count": 1
}
```

| `faultType` | Поведение |
|---|---|
| `HTTP_STATUS` | Ответ с `httpStatus` (нужен в теле правила) |
| `DELAY` | Пауза `delayMs`, затем обычная обработка |
| `TIMEOUT` | Долгое ожидание, затем 503 |
| `CONNECTION_RESET` | Обрыв ответа |
| `LOSE_RESPONSE` | Изменения в БД есть, HTTP-ответ клиенту не отдаётся |

`pathPattern`: `*` и `**`. Фильтр: `RemapFaultFilter` только на `/api/remap/1.2/**`.

---

## Сервисный слой заглушки (ориентир, не вызывать из процесса)

| Класс | Метод | Смысл |
|---|---|---|
| `RemapProductService` | `getById` | Ответ GET product |
| `RemapCustomerOrderService` | `getById` / `getPositions` / `getPosition` | Чтение заказа |
| `RemapCustomerOrderService` | `updatePosition` / `updateOrder` | Резерв и правки позиций |
| `RemapStockService` | `reportAll` | Отчёт остатков |
| `RemapStockService` | `applyReserveDelta` / `applyShip` / `restoreShip` | Внутренняя смена остатков при reserve / проведении |
| `RemapDemandService` | `create` / `getById` / `getPositions` / `update` / `delete` | Жизненный цикл отгрузки |
| `RemapStubSeedService` | `resetToSeed` | Пересоздать тестовые данные |
| `RemapFaultService` | `addRule` / `consumeMatchingRule` / `clearRules` | Управление сбоями |

Студенческий процесс ходит в заглушку **только по HTTP/SDK**, не через эти Spring-бины напрямую.

---

## Состояние и тестовые данные

Таблицы: `remap_stub_*` (Liquibase `005-remap-stub.yaml`). Общая БД → общее состояние у нескольких инстансов. Резервы/остатки — pessimistic lock.

UUID: `ru.moysklad.intern.remapstub.seed.RemapStubIds`.

| Константа | Для какого сценария задания |
|---|---|
| `ORDER_SINGLE` + `PRODUCT_SUFFICIENT` | Успешный путь (этапы 1–8) |
| `ORDER_MULTI` | Несколько позиций |
| `ORDER_INSUFFICIENT` + `PRODUCT_ZERO` | Этап 2: ожидание / нехватка остатков |
| `ORDER_WITH_RESERVE` + `PRODUCT_PARTIAL` | Уже есть резерв; проверка available |
| `ORDER_WITH_DEMAND` + `DEMAND_EXISTING` | У заказа уже есть черновик отгрузки (осторожно с дублями на этапе 5) |
| `ORGANIZATION_ID` / `COUNTERPARTY_ID` / `STORE_ID` | Обязательные ссылки при `POST /entity/demand` |

Сброс: `POST /api/stub/reset`.

---

## Ограничения SDK 9.2

1. Нет клиента `report/stock/*` — остатки через HTTP/`RestClient`.
2. `fields=stock` нельзя передать через `ApiParam` — для typed API используйте отчёт остатков.
3. Host: `new ApiClient("localhost:8080", false, login, password)`.

---

## Что заглушка не делает

Не пишет BPMN, делегаты, отмену Camunda и идемпотентность процесса. Два `POST /entity/demand` → два документа. Заглушка лишь создаёт условия для проверки вашего решения.
