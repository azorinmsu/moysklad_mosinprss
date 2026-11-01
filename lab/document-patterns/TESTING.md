# Тестирование

Все тесты лабораторной — быстрые unit-тесты на JUnit 5 и AssertJ. Spring-контекст, Docker, PostgreSQL,
Kafka и Camunda не нужны.

## Команды

```bash
cd intern

# все тесты лабораторной
./mvnw test -Dtest='ru.moysklad.intern.documentpatterns.**'

# один паттерн
./mvnw test -Dtest='ru.moysklad.intern.documentpatterns.strategy.**'

# один класс / один метод
./mvnw test -Dtest=DocumentWorkflowTest
./mvnw test -Dtest='DocumentWorkflowTest#fullDocumentLifecycle'

# только компиляция (включая тесты)
./mvnw test-compile
```

Если зависимости уже в `~/.m2`, можно добавить `-o` (офлайн-режим).

## Что ожидать

| Момент | Результат |
|---|---|
| До начала работы | 92 теста: 22 проходят (инфраструктура), 70 пропущены (`@Disabled`), 0 падений |
| После выполнения | 92 теста проходят, 0 пропущено, 0 падений |

### Готовые инфраструктурные тесты (не менять)

- `repository/InMemoryStockRepositoryTest` (10) — остатки, атомарность пакета движений, журнал, конкурентный доступ.
- `DocumentPatternsInfrastructureTest` (12) — модель документа, `DocumentContext`, `ValidationContext`,
  связывание цепочки, реестр фабрик, `AuditTrail`, тестовые утилиты.

Если они падают до начала работы — проблема в окружении, а не в вашем коде.

### Тесты-заготовки (пишет студент)

| Класс | Тестов | Паттерн |
|---|---:|---|
| `factory/DocumentFactoryTest` | 8 | Factory Method / Abstract Factory |
| `strategy/StockMovementStrategyTest` | 6 | Strategy |
| `validation/DocumentValidationChainTest` | 9 | Chain of Responsibility |
| `processing/DocumentProcessorTest` | 8 | Template Method |
| `decorator/DocumentDecoratorTest` | 7 (1 дополнительный) | Decorator |
| `command/DocumentCommandTest` | 8 | Command |
| `state/DocumentStateTest` | 7 | State |
| `observer/DocumentObserverTest` | 6 | Observer |
| `builder/DocumentBuilderTest` | 7 | Builder |
| `DocumentWorkflowTest` | 4 | Сквозной сценарий |

Каждый метод помечен `@Disabled("TODO: implement test")`, содержит комментарии `TODO` с тем, что проверить,
и `fail(...)`. Порядок работы:

1. Снимите `@Disabled`, удалите `fail(...)`, напишите arrange / act / assert по подсказкам.
2. Запустите тест — он должен упасть с `UnsupportedOperationException: TODO` (красный).
3. Реализуйте методы с `TODO` в основном коде — тест становится зелёным.
4. Не ослабляйте проверки ради прохождения теста.

## Тестовые утилиты (`src/test/java/.../documentpatterns/support`)

| Класс | Назначение |
|---|---|
| `TestDocuments` | Товары, склады, партии со случайными `id`; `qty("10")`; `position(...)`; готовые черновики `acceptance(...)`, `shipment(...)`, `transfer(...)` (через конструкторы модели, без фабрик и builder) |
| `TestStocks` | `empty()`, `withQuantity(product, warehouse, "20")`, `workflowInitialStocks(x, y, a, b)` — остатки сквозного сценария (A: X=20, Y=10) |
| `RecordingEventListener` | Подписчик-шпион: `events()`, `types()`, `count(type)` |
| `RecordingValidator` | Звено-шпион: `invocations()`; `failingWith(code)` — звено, которое всегда падает |
| `FailingStockRepository` | Обёртка хранилища, `apply` которой бросает заданное исключение; `applyAttempts()` |

Шпионы и обёртки позволяют проверять порядок вызовов и отсутствие побочных эффектов без mock-фреймворков.

## Самопроверка перед сдачей

```bash
cd intern
./mvnw test -Dtest='ru.moysklad.intern.documentpatterns.**'      # 92 passed, 0 skipped

grep -rn "TODO" src/main/java/ru/moysklad/intern/documentpatterns   # пусто
grep -rn "@Disabled\|fail(\"TODO" src/test/java/ru/moysklad/intern/documentpatterns   # пусто

./mvnw test-compile                                                  # весь модуль компилируется
```

## Что проверяет сквозной сценарий

```text
A: X=20, Y=10; B: 0
приёмка 10 X на A           → A/X = 30
отгрузка 7 X с A            → A/X = 23
перемещение 5 X с A на B    → A/X = 18, B/X = 5
отмена перемещения          → A/X = 23, B/X = 0
отгрузка 100 X с A          → INSUFFICIENT_STOCK; A/X = 23; документ не POSTED; нет DOCUMENT_POSTED
```
