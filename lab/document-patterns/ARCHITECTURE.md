# Архитектура лабораторной

Базовый пакет: `ru.moysklad.intern.documentpatterns`.

| Пакет | Назначение | Паттерн |
|---|---|---|
| `model` | Документы, позиции, движения, статусы, коды ошибок | — |
| `repository` | Хранилище остатков в памяти, журнал движений | — (инфраструктура) |
| `factory` | Создание документа и семейства его объектов | Factory Method, Abstract Factory |
| `builder` | Пошаговое создание документа | Builder |
| `strategy` | Расчёт движений по типу документа | Strategy |
| `validation` | Проверки перед проведением | Chain of Responsibility |
| `processing` | Алгоритм проведения | Template Method |
| `decorator` | Логирование, аудит, замер времени | Decorator |
| `command` | Проведение и отмена как объекты-операции | Command |
| `state` | Жизненный цикл документа | State |
| `observer` | События документов и подписчики | Observer |
| `service` | Точка входа, объединяющая паттерны | Facade-подобный сервис |

## Единый маршрут выполнения

Каждый паттерн отвечает за одну задачу и вызывается ровно в одном месте.

```text
DocumentService.create(type, spec)
  → DocumentFactoryRegistry.forType(type)          (поиск фабрики)
  → DocumentBuilder.forFactory(factory) … build()   (Builder → Factory Method)
  → publish(DOCUMENT_CREATED)                       (Observer)

DocumentService.post(document, operationId)
  → DocumentCommandExecutor.execute(PostDocumentCommand)      (Command, идемпотентность)
    → AuditDecorator → LoggingDecorator                         (Decorator)
      → AbstractDocumentProcessor.process(document)             (Template Method)
          0. null-проверка
          1. DocumentContext.post()                              (State: планирование перехода)
          2. DocumentValidationChain.validate + validateSpecific (Chain of Responsibility)
          3. ValidationContext.plannedMovements()                (Strategy, вызывается один раз)
          4. StockRepository.apply(movements)                    (атомарно)
          5. DocumentContext.commit()                            (статус POSTED)
          6. document.recordPostedMovements(movements)
          7. publish(DOCUMENT_POSTED)                            (Observer)

DocumentService.unpost(document, operationId)
  → DocumentCommandExecutor.execute(UnpostDocumentCommand)
      1. DocumentContext.unpost()                                (State)
      2. обратные движения из getPostedMovements()
      3. StockRepository.apply(reversed)                         (атомарно)
      4. DocumentContext.commit()                                (статус CANCELLED)
      5. publish(DOCUMENT_UNPOSTED)                              (Observer)
```

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant S as DocumentService
    participant E as DocumentCommandExecutor
    participant C as PostDocumentCommand
    participant A as AuditDecorator
    participant L as LoggingDecorator
    participant P as AbstractDocumentProcessor
    participant St as DocumentContext (State)
    participant V as DocumentValidationChain
    participant VC as ValidationContext
    participant G as StockMovementStrategy
    participant R as StockRepository
    participant O as DocumentEventPublisher

    Client->>S: post(document, operationId)
    S->>E: execute(new PostDocumentCommand(...))
    E->>E: operationId уже успешно выполнен? → SKIPPED_DUPLICATE
    E->>C: execute()
    C->>A: execute(document)
    A->>L: execute(document)
    L->>P: execute(document) → process(document)
    P->>St: post() — DraftState планирует POSTED
    P->>V: validate(document, validationContext)
    V->>VC: plannedMovements() (StockAvailabilityValidator)
    VC->>G: calculateMovements(document) — один раз
    P->>P: validateSpecific(document)
    P->>VC: plannedMovements() — из кэша
    P->>R: apply(movements) — всё или ничего
    P->>St: commit() — документ POSTED
    P->>O: publish(DOCUMENT_POSTED)
    O-->>O: AuditDocumentListener, NotificationDocumentListener
    L-->>A: успех (лог)
    A-->>C: AuditRecord SUCCESS
    E-->>S: EXECUTED (история операций)
```

При ошибке на шагах 1–6 `commit()` не вызывается (статус прежний), `apply` не изменил хранилище,
публикуется `DOCUMENT_PROCESSING_FAILED`, исключение проходит через декораторы (лог ошибки, `AuditRecord FAILURE`)
и перехватывается исполнителем (`CommandOutcome.FAILED`, запись в истории). `DOCUMENT_POSTED` не публикуется.

### Осознанные проектные решения

- **Статус проверяет State, а не цепочка.** Правила жизненного цикла живут только в классах состояний; отдельное звено
  «допустимый статус» продублировало бы их. Состояние вызывается первым шагом шаблона, до цепочки.
- **Двухфазная смена статуса.** Состояние только планирует переход (`transitionTo`), `commit()` применяет его после
  успешного изменения остатков. Иначе ошибка остатков оставила бы документ `POSTED` без движений.
- **Стратегия вызывается один раз.** `ValidationContext` лениво вычисляет и запоминает плановые движения:
  их используют и `StockAvailabilityValidator`, и шаг применения. Если цепочка упала раньше (пустой документ),
  движения не рассчитываются вовсе.
- **Двойная проверка остатков.** Цепочка даёт понятную ошибку заранее, а `InMemoryStockRepository.apply`
  повторно проверяет итоговые остатки под блокировкой — это защищает от гонки между проверкой и записью.
- **Четыре журнала с разной ответственностью:** `AuditTrail` (попытки операций, Decorator), журнал
  `AuditDocumentListener` (события, Observer), история исполнителя (операции по `operationId`, Command),
  история хранилища (движения остатков).

---

## 1. Factory Method и Abstract Factory

```mermaid
classDiagram
    class DocumentFactory {
        <<abstract>>
        +createDocument(UUID id, LocalDateTime moment) ErpDocument «final»
        +documentType()* DocumentsType
        #newDocument(UUID id, LocalDateTime moment)* ErpDocument
        +createMovementStrategy()* StockMovementStrategy
        +createProcessor(ProcessingInfrastructure)* AbstractDocumentProcessor
    }
    class AcceptanceDocumentFactory
    class ShipmentDocumentFactory
    class TransferDocumentFactory
    class DocumentFactoryRegistry {
        +forType(DocumentsType) DocumentFactory
        +supportedTypes() Set~DocumentsType~
    }
    class ErpDocument {
        <<abstract>>
    }
    DocumentFactory <|-- AcceptanceDocumentFactory
    DocumentFactory <|-- ShipmentDocumentFactory
    DocumentFactory <|-- TransferDocumentFactory
    DocumentFactoryRegistry o-- DocumentFactory
    AcceptanceDocumentFactory ..> AcceptanceDocument : newDocument
    AcceptanceDocumentFactory ..> AcceptanceMovementStrategy
    AcceptanceDocumentFactory ..> AcceptanceProcessor
    ShipmentDocumentFactory ..> ShipmentDocument
    TransferDocumentFactory ..> TransferDocument
    ErpDocument <|-- AcceptanceDocument
    ErpDocument <|-- ShipmentDocument
    ErpDocument <|-- TransferDocument
```

`createDocument` — общий алгоритм (проверка аргументов, вызов фабричного метода, контроль результата),
`newDocument` — фабричный метод. Методы `newDocument`, `createMovementStrategy`, `createProcessor` вместе образуют
Abstract Factory: подкласс создаёт *согласованное семейство* объектов одного типа документа.

`DocumentFactoryRegistry` — справочник «тип → фабрика», по сути Simple Factory. Это инфраструктура, а не Factory Method.

**Почему Factory Method помогает расширению.** Чтобы добавить документ «Списание», пишут `WriteOffDocument`,
`WriteOffMovementStrategy`, `WriteOffProcessor`, `WriteOffDocumentFactory` и передают фабрику в конструктор
`DocumentService`. Сервис, реестр, цепочка проверок, декораторы, команды и состояния не меняются. Сервис работает
только с абстракциями `DocumentFactory` и `DocumentOperation` — новый тип не требует нового `case`.

## 2. Strategy

```mermaid
classDiagram
    class StockMovementStrategy {
        <<interface>>
        +calculateMovements(ErpDocument) List~StockMovement~
    }
    class AcceptanceMovementStrategy
    class ShipmentMovementStrategy
    class TransferMovementStrategy
    class StockMovement {
        <<record>>
        documentId
        product
        warehouse
        lot
        delta
    }
    StockMovementStrategy <|.. AcceptanceMovementStrategy
    StockMovementStrategy <|.. ShipmentMovementStrategy
    StockMovementStrategy <|.. TransferMovementStrategy
    StockMovementStrategy ..> StockMovement : создаёт
    AbstractDocumentProcessor <|-- AcceptanceProcessor
    AcceptanceProcessor o-- StockMovementStrategy : calculateMovements делегирует
```

Стратегии — чистые функции «документ → движения», без доступа к хранилищу. Применение движений выполняет
только шаблонный метод.

## 3. Chain of Responsibility

```mermaid
classDiagram
    class DocumentValidator {
        <<interface>>
        +validate(ErpDocument, ValidationContext)
    }
    class AbstractDocumentValidator {
        <<abstract>>
        -next AbstractDocumentValidator
        +linkWith(next) next
        +validate(ErpDocument, ValidationContext) «final»
        #check(ErpDocument, ValidationContext)*
    }
    class DocumentValidationChain {
        +of(AbstractDocumentValidator...)$ DocumentValidationChain
        +defaultChain()$ DocumentValidationChain
        +links() List
    }
    class ValidationContext {
        +availableQuantity(StockKey) BigDecimal
        +plannedMovements() List~StockMovement~
        +movementsCalculated() boolean
    }
    DocumentValidator <|.. AbstractDocumentValidator
    DocumentValidator <|.. DocumentValidationChain
    AbstractDocumentValidator --> AbstractDocumentValidator : next
    AbstractDocumentValidator <|-- PositionValidator
    AbstractDocumentValidator <|-- QuantityValidator
    AbstractDocumentValidator <|-- WarehouseValidator
    AbstractDocumentValidator <|-- StockAvailabilityValidator
    DocumentValidationChain o-- AbstractDocumentValidator
    StockAvailabilityValidator ..> ValidationContext
```

```mermaid
flowchart LR
    P[PositionValidator<br/>EMPTY_DOCUMENT] --> Q[QuantityValidator<br/>INVALID_QUANTITY]
    Q --> W[WarehouseValidator<br/>MISSING_*_WAREHOUSE, SAME_WAREHOUSES]
    W --> S[StockAvailabilityValidator<br/>INSUFFICIENT_STOCK]
```

Ошибка звена — исключение `DocumentValidationException`, которое прерывает цепочку.

## 4. Template Method

```mermaid
classDiagram
    class DocumentOperation {
        <<interface>>
        +execute(ErpDocument)
    }
    class AbstractDocumentProcessor {
        <<abstract>>
        #validationChain DocumentValidator
        #stockRepository StockRepository
        #eventPublisher DocumentEventPublisher
        +process(ErpDocument) «final»
        +execute(ErpDocument) «final»
        #validateSpecific(ErpDocument)*
        #calculateMovements(ErpDocument)* List~StockMovement~
    }
    class ProcessingInfrastructure {
        <<record>>
        validationChain
        stockRepository
        eventPublisher
    }
    DocumentOperation <|.. AbstractDocumentProcessor
    AbstractDocumentProcessor <|-- AcceptanceProcessor
    AbstractDocumentProcessor <|-- ShipmentProcessor
    AbstractDocumentProcessor <|-- TransferProcessor
    AbstractDocumentProcessor ..> ProcessingInfrastructure
```

```mermaid
flowchart TD
    A[process document] --> B{document == null?}
    B -- да --> X1[DOCUMENT_REQUIRED]
    B -- нет --> C[DocumentContext.post — State]
    C --> D[validationChain.validate]
    D --> E[validateSpecific — крючок подкласса]
    E --> F[plannedMovements — calculateMovements подкласса]
    F --> G[stockRepository.apply — атомарно]
    G --> H[DocumentContext.commit — POSTED]
    H --> I[recordPostedMovements]
    I --> J[publish DOCUMENT_POSTED]
    C -. ошибка .-> Z[publish DOCUMENT_PROCESSING_FAILED, проброс]
    D -. ошибка .-> Z
    E -. ошибка .-> Z
    G -. ошибка .-> Z
```

## 5. Decorator

```mermaid
classDiagram
    class DocumentOperation {
        <<interface>>
        +execute(ErpDocument)
    }
    class DocumentOperationDecorator {
        <<abstract>>
        #delegate DocumentOperation
    }
    class LoggingDecorator {
        +execute(ErpDocument)
    }
    class AuditDecorator {
        +execute(ErpDocument)
    }
    class TimingDecorator {
        +execute(ErpDocument)
    }
    class AuditTrail {
        +record(AuditRecord)
        +records() List~AuditRecord~
    }
    DocumentOperation <|.. DocumentOperationDecorator
    DocumentOperation <|.. AbstractDocumentProcessor
    DocumentOperationDecorator o-- DocumentOperation : delegate
    DocumentOperationDecorator <|-- LoggingDecorator
    DocumentOperationDecorator <|-- AuditDecorator
    DocumentOperationDecorator <|-- TimingDecorator
    AuditDecorator --> AuditTrail
```

В `DocumentService` обработчик оборачивается так:
`new AuditDecorator(new LoggingDecorator(processor, log), auditTrail, "POST")`.

## 6. Command

```mermaid
classDiagram
    class DocumentCommand {
        <<interface>>
        +operationId() UUID
        +name() String
        +document() ErpDocument
        +execute()
    }
    class PostDocumentCommand {
        -postingOperation DocumentOperation
    }
    class UnpostDocumentCommand {
        -stockRepository StockRepository
        -eventPublisher DocumentEventPublisher
    }
    class DocumentCommandExecutor {
        +execute(DocumentCommand) CommandOutcome
        +history() List~CommandHistoryEntry~
    }
    class CommandOutcome {
        <<enumeration>>
        EXECUTED
        SKIPPED_DUPLICATE
        FAILED
    }
    DocumentCommand <|.. PostDocumentCommand
    DocumentCommand <|.. UnpostDocumentCommand
    DocumentCommandExecutor ..> DocumentCommand : execute
    DocumentCommandExecutor ..> CommandOutcome
    PostDocumentCommand --> DocumentOperation
```

Отмена проведения — компенсирующая операция: в журнал остатков добавляются обратные движения, история не стирается.

## 7. State

```mermaid
stateDiagram-v2
    [*] --> DRAFT : createDocument
    DRAFT --> POSTED : post (DraftState)
    POSTED --> CANCELLED : unpost (PostedState)
    DRAFT --> DRAFT : unpost → NOT_POSTED
    POSTED --> POSTED : post → ALREADY_POSTED
    CANCELLED --> CANCELLED : post / unpost → ALREADY_CANCELLED
```

```mermaid
classDiagram
    class DocumentState {
        <<interface>>
        +status() DocumentStatus
        +post(DocumentContext)
        +unpost(DocumentContext)
    }
    class DocumentContext {
        +currentState() DocumentState
        +post()
        +unpost()
        +transitionTo(DocumentStatus)
        +plannedStatus() Optional
        +commit()
    }
    DocumentState <|.. DraftState
    DocumentState <|.. PostedState
    DocumentState <|.. CancelledState
    DocumentContext ..> DocumentState : делегирует
    DocumentContext --> ErpDocument
```

**`CANCELLED`, удаление и отмена заказа — разные вещи.**

- `CANCELLED` (учебная модель) — проведение отменено: движения сторнированы обратными, документ остаётся
  в системе с полной историей.
- Удаление — документ убирается из учёта (в реальном МойСклад — в корзину); для проведённого документа сначала
  нужно снять проведение.
- Бизнес-отмена заказа покупателя — статус заказа в его собственном процессе (например, «Отменён»); заказ может
  вообще не создавать движений остатков, а связанные отгрузки отменяются отдельно.

## 8. Observer

```mermaid
classDiagram
    class DocumentEventPublisher {
        +subscribe(DocumentEventListener)
        +unsubscribe(DocumentEventListener)
        +publish(DocumentEvent)
        +deliveryFailures() List~DeliveryFailure~
    }
    class DocumentEventListener {
        <<interface>>
        +onEvent(DocumentEvent)
    }
    class DocumentEvent {
        <<record>>
        eventId
        type
        documentId
        documentType
        documentStatus
        errorCode
        occurredAt
    }
    class NotificationSender {
        <<interface>>
        +send(String)
    }
    DocumentEventPublisher o-- DocumentEventListener
    DocumentEventListener <|.. AuditDocumentListener
    DocumentEventListener <|.. NotificationDocumentListener
    NotificationDocumentListener --> NotificationSender
    DocumentEventPublisher ..> DocumentEvent
```

**Синхронный Observer и transactional outbox.** В лабораторной событие публикуется синхронно в том же потоке
после фиксации изменений в памяти. В системе с БД так делать нельзя: между фиксацией транзакции и доставкой
процесс может упасть (событие потеряно), а доставка до коммита может сообщить о том, что потом откатится.
Transactional outbox решает это: событие пишется в таблицу (в проекте — `outbox_event`) в той же транзакции,
что и изменения документа и остатков, а отдельный публикатор доставляет его в брокер с повторами
(«как минимум один раз»), поэтому подписчики должны быть идемпотентными по `eventId` — как в этой лабораторной.

## 9. Builder

```mermaid
classDiagram
    class DocumentBuilder {
        -factory DocumentFactory
        +acceptance()$ DocumentBuilder
        +shipment()$ DocumentBuilder
        +transfer()$ DocumentBuilder
        +forFactory(DocumentFactory)$ DocumentBuilder
        +id(UUID) DocumentBuilder
        +moment(LocalDateTime) DocumentBuilder
        +sourceWarehouse(Warehouse) DocumentBuilder
        +targetWarehouse(Warehouse) DocumentBuilder
        +addPosition(Product, BigDecimal) DocumentBuilder
        +addPosition(Product, BigDecimal, Lot) DocumentBuilder
        +build() ErpDocument
    }
    DocumentBuilder --> DocumentFactory : createDocument
    DocumentFactory ..> ErpDocument
```

Builder отвечает за сборку и обязательные поля, Factory — за выбор конкретного класса документа.

## Объединение: `DocumentService`

```mermaid
classDiagram
    class DocumentService {
        +DocumentService(List~DocumentFactory~, StockRepository, DocumentEventPublisher, AuditTrail, Consumer~String~)
        +withDefaultDocuments(...)$ DocumentService
        +create(DocumentsType, Consumer~DocumentBuilder~) ErpDocument
        +post(ErpDocument, UUID) CommandOutcome
        +unpost(ErpDocument, UUID) CommandOutcome
        +operationHistory() List~CommandHistoryEntry~
    }
    DocumentService --> DocumentFactoryRegistry
    DocumentService --> DocumentCommandExecutor
    DocumentService --> StockRepository
    DocumentService --> DocumentEventPublisher
    DocumentService ..> DocumentBuilder
    DocumentService ..> PostDocumentCommand
    DocumentService ..> UnpostDocumentCommand
    DocumentService o-- DocumentOperation : декорированный обработчик по типу
```

## Хранилище остатков (готово)

`InMemoryStockRepository` хранит остатки по ключу `StockKey(productId, warehouseId, lotId)`:

- `apply(movements)` под одной блокировкой суммирует дельты по ключу, проверяет, что ни один итоговый остаток
  не станет отрицательным, и только потом применяет все движения (всё или ничего);
- нехватка — `InsufficientStockException` (код `INSUFFICIENT_STOCK`), хранилище не меняется;
- каждое применённое движение попадает в журнал `history()` с порядковым номером и остатком после движения.
