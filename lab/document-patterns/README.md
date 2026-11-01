# Лабораторная «Архитектура обработки ERP-документов с использованием паттернов проектирования»

Учебная система обработки складских документов МойСклад: создание, заполнение, проверка, проведение,
движения товаров, отмена проведения, история и уведомления.

**Главная архитектурная задача:** добавление нового типа документа не должно требовать переписывания
центрального сервиса обработки.

| Файл | Содержание |
|---|---|
| [ASSIGNMENT.md](ASSIGNMENT.md) | Задания по каждому паттерну, критерии приёмки, баллы |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Архитектура, маршрут выполнения, UML-диаграммы (Mermaid) |
| [TESTING.md](TESTING.md) | Как запускать и писать тесты, что ожидать до и после работы |
| [QUESTIONS.md](QUESTIONS.md) | Вопросы для защиты |

## Где код

```text
intern/src/main/java/ru/moysklad/intern/documentpatterns/   # код лабораторной
intern/src/test/java/ru/moysklad/intern/documentpatterns/   # тесты и тестовые утилиты
```

Лабораторная изолирована:

- не использует Spring-контекст, PostgreSQL, Kafka и Camunda — все тесты быстрые unit-тесты;
- не объявляет Spring-бинов и не меняет существующую бизнес-логику, миграции и API;
- не зависит от предыдущей лабораторной `lab/design-patterns` (пакет `ru.moysklad.intern.patterns`).

Из существующей модели переиспользуются без изменений: `Product`, `Warehouse`, `Lot` (как обычные объекты)
и перечисление типов `DocumentsType` (`GOODS_ACCEPTANCE`, `SHIPMENT`, `SHIFT`, `MACHINING`, `MAKE`).
JPA-сущность `DocumentsWithPositions` не используется напрямую: её позиции привязаны к партии и серийным номерам
и не содержат товара и количества. Адаптер к ней — задание повышенной сложности.

## Быстрый старт

```bash
cd intern
./mvnw test -Dtest='ru.moysklad.intern.documentpatterns.**'
```

До начала работы: 22 инфраструктурных теста проходят, 70 студенческих пропущены (`@Disabled`).
После выполнения: 92 теста проходят, 0 пропущено.

## Что готово, а что делает студент

| Готово (инфраструктура) | Реализует студент (`TODO`) |
|---|---|
| Модель: `ErpDocument` и подклассы, `DocumentPosition`, `StockMovement`, `DocumentStatus`, `DocumentErrorCode` | Фабрики документов (Factory Method + Abstract Factory) |
| `InMemoryStockRepository` — потокобезопасные атомарные остатки и журнал движений | Стратегии расчёта движений |
| `ValidationContext`, механика звеньев `AbstractDocumentValidator`, `DocumentValidationChain.of` | Звенья цепочки проверок и её сборка |
| `DocumentContext` — двухфазная смена статуса | Алгоритм проведения (Template Method) и крючки обработчиков |
| `DocumentEvent`, `AuditTrail`, `DocumentFactoryRegistry`, записи истории | Декораторы логирования и аудита |
| Тестовые утилиты `TestDocuments`, `TestStocks`, «шпионы» | Команды проведения и отмены, исполнитель команд |
| Инфраструктурные тесты | Состояния жизненного цикла, публикатор и подписчики, Builder, `DocumentService` |
