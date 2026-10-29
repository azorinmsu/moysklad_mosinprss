# Архитектура лабораторной «Паттерны проектирования»

Пакет: `ru.moysklad.intern.patterns` (модуль `intern`).
Диаграммы отражают классы из репозитория; методы с `TODO` помечены комментарием `%% TODO` рядом с классом.

## Место в проекте

Лабораторная изолирована от остального приложения:

- нет Spring-аннотаций — классы не попадают в контекст и не влияют на запуск приложения
  и интеграционные тесты (иначе вызов незавершённого `createDefault()` при старте сломал бы контекст);
- нет JPA, Kafka и Camunda; единственная связь с доменом — `OrderItem.fromProduct(Product, quantity)`;
- при желании сервис позже регистрируется как bean через `@Configuration`, так же как остальные сервисы проекта
  (зависимости уже передаются через конструктор).

## Общая схема расчёта заказа

```mermaid
sequenceDiagram
    autonumber
    participant C as Клиент
    participant S as OrderCalculationService
    participant V as OrderValidationChain
    participant F as DiscountStrategyFactory
    participant P as PriceCalculator (декорированный)

    C->>S: calculate(OrderDraft)
    S->>V: validate(order)
    Note over V: Empty → Quantity → Price → TotalAmount<br/>первая ошибка — OrderValidationException
    S->>F: create(order.customerType())
    F-->>S: DiscountStrategy
    S->>S: new BasePriceCalculator(strategy)<br/>+ декораторы по order.additionalServices()
    S->>P: calculate(order)
    P-->>S: total
    S-->>C: OrderCalculation(subtotal, discount, subtotalAfterDiscount,<br/>deliveryCost, packagingCost, total)
```

```mermaid
flowchart LR
    A[OrderDraft] --> B{Цепочка проверок}
    B -- ошибка --> X[OrderValidationException]
    B -- ок --> D[Фабрика: стратегия скидки]
    D --> E[BasePriceCalculator<br/>товары − скидка]
    E --> F[DeliveryDecorator<br/>+500 ₽]
    F --> G[PackagingDecorator<br/>+200 ₽]
    G --> H[OrderCalculation]
```

## Модель

```mermaid
classDiagram
    class CustomerType {
        <<enumeration>>
        REGULAR
        WHOLESALE
        VIP
        CORPORATE
    }
    class AdditionalService {
        <<enumeration>>
        DELIVERY
        GIFT_PACKAGING
        +title() String
        +price() BigDecimal
    }
    class Money {
        <<utility>>
        +SCALE = 2
        +ROUNDING = HALF_UP
        +ZERO
        +round(BigDecimal) BigDecimal
        +of(String) BigDecimal
    }
    class OrderItem {
        <<record>>
        +productId UUID
        +productName String
        +unitPrice BigDecimal
        +quantity BigDecimal
        +fromProduct(Product, BigDecimal)$ OrderItem
        +lineTotal() BigDecimal
    }
    class OrderDraft {
        <<record>>
        +orderId UUID
        +customerType CustomerType
        +items List~OrderItem~
        +additionalServices Set~AdditionalService~
        +hasService(AdditionalService) boolean
        +subtotal() BigDecimal
    }
    class OrderCalculation {
        <<record>>
        +subtotal BigDecimal
        +discount BigDecimal
        +subtotalAfterDiscount BigDecimal
        +deliveryCost BigDecimal
        +packagingCost BigDecimal
        +total BigDecimal
    }
    OrderDraft --> CustomerType
    OrderDraft "1" *-- "*" OrderItem
    OrderDraft --> AdditionalService
```

## Strategy

```mermaid
classDiagram
    class DiscountStrategy {
        <<interface>>
        +calculateDiscount(BigDecimal subtotal) BigDecimal
    }
    class RegularDiscountStrategy {
        +calculateDiscount(BigDecimal) BigDecimal
    }
    class WholesaleDiscountStrategy {
        +THRESHOLD = 50000.00$
        +RATE = 0.10$
        +calculateDiscount(BigDecimal) BigDecimal
    }
    class VipDiscountStrategy {
        +RATE = 0.15$
        +calculateDiscount(BigDecimal) BigDecimal
    }
    class BasePriceCalculator {
        -discountStrategy DiscountStrategy
    }
    DiscountStrategy <|.. RegularDiscountStrategy
    DiscountStrategy <|.. WholesaleDiscountStrategy
    DiscountStrategy <|.. VipDiscountStrategy
    BasePriceCalculator o--> DiscountStrategy : контекст стратегии
    %% TODO: calculateDiscount во всех трёх реализациях
```

Контекст стратегии — `BasePriceCalculator`: он получает стратегию через конструктор и не знает, какая именно подставлена.

## Factory

```mermaid
classDiagram
    class DiscountStrategyFactory {
        +create(CustomerType) DiscountStrategy
    }
    class UnsupportedCustomerTypeException {
        +getCustomerType() CustomerType
    }
    class OrderCalculationService
    DiscountStrategyFactory ..> RegularDiscountStrategy : создаёт
    DiscountStrategyFactory ..> WholesaleDiscountStrategy : создаёт
    DiscountStrategyFactory ..> VipDiscountStrategy : создаёт
    DiscountStrategyFactory ..> UnsupportedCustomerTypeException : бросает
    DiscountStrategyFactory ..> DiscountStrategy : возвращает
    OrderCalculationService --> DiscountStrategyFactory
    %% TODO: create
```

### Factory Method и Simple Factory

| | Simple Factory (эта лабораторная) | GoF Factory Method |
|---|---|---|
| Структура | Один класс с методом `create(параметр)` | Абстрактный «создатель» с фабричным методом и подклассы-создатели |
| Как выбирается продукт | По параметру (`switch` / реестр) | Подклассом, который переопределяет фабричный метод |
| Расширение | Правка одного метода фабрики (или регистрация в реестре) | Новый подкласс-создатель без правки существующих |
| Статус | Идиома, не входит в каталог GoF | Порождающий паттерн GoF |

`DiscountStrategyFactory` — **Simple Factory**. Она решает главную задачу лабораторной: убирает `new ConcreteStrategy()`
из бизнес-сервиса и собирает выбор стратегии в одном месте. Полноценный Factory Method выглядел бы так:

```mermaid
classDiagram
    class OrderPricingCreator {
        <<abstract>>
        #createDiscountStrategy()* DiscountStrategy
        +price(OrderDraft) BigDecimal
    }
    class VipOrderPricingCreator {
        #createDiscountStrategy() DiscountStrategy
    }
    class WholesaleOrderPricingCreator {
        #createDiscountStrategy() DiscountStrategy
    }
    OrderPricingCreator <|-- VipOrderPricingCreator
    OrderPricingCreator <|-- WholesaleOrderPricingCreator
    OrderPricingCreator ..> DiscountStrategy
```

Здесь `price(...)` — общий алгоритм создателя, а какой продукт он получит, решает подкласс. В коде лабораторной этой иерархии нет,
схема приведена для сравнения на защите.

## Chain of Responsibility

```mermaid
classDiagram
    class OrderValidator {
        <<interface>>
        +validate(OrderDraft)
    }
    class AbstractOrderValidator {
        <<abstract>>
        -next OrderValidator
        +linkWith(T next) T
        #passToNext(OrderDraft)
    }
    class EmptyOrderValidator
    class QuantityValidator
    class PriceValidator
    class TotalAmountValidator {
        +MAX_TOTAL_AMOUNT = 1000000.00$
    }
    class OrderValidationChain {
        -head OrderValidator
        +OrderValidationChain(OrderValidator head)
        +createDefault()$ OrderValidationChain
        +validate(OrderDraft)
    }
    class OrderValidationException {
        +getCode() ValidationErrorCode
    }
    class ValidationErrorCode {
        <<enumeration>>
        EMPTY_ORDER
        INVALID_QUANTITY
        INVALID_PRICE
        TOTAL_AMOUNT_EXCEEDED
    }
    OrderValidator <|.. AbstractOrderValidator
    AbstractOrderValidator <|-- EmptyOrderValidator
    AbstractOrderValidator <|-- QuantityValidator
    AbstractOrderValidator <|-- PriceValidator
    AbstractOrderValidator <|-- TotalAmountValidator
    AbstractOrderValidator o--> OrderValidator : next
    OrderValidationChain --> OrderValidator : head
    OrderValidationException --> ValidationErrorCode
    %% TODO: validate в четырёх звеньях, createDefault
```

Объекты стандартной цепочки во время выполнения:

```mermaid
flowchart LR
    S[OrderValidationChain] --> E[EmptyOrderValidator]
    E -- passToNext --> Q[QuantityValidator]
    Q -- passToNext --> P[PriceValidator]
    P -- passToNext --> T[TotalAmountValidator]
    E -. EMPTY_ORDER .-> X((OrderValidationException))
    Q -. INVALID_QUANTITY .-> X
    P -. INVALID_PRICE .-> X
    T -. TOTAL_AMOUNT_EXCEEDED .-> X
```

## Decorator

```mermaid
classDiagram
    class PriceCalculator {
        <<interface>>
        +calculate(OrderDraft) BigDecimal
    }
    class BasePriceCalculator {
        -discountStrategy DiscountStrategy
        +calculate(OrderDraft) BigDecimal
    }
    class PriceCalculatorDecorator {
        <<abstract>>
        #delegate PriceCalculator
        #service AdditionalService
        +service() AdditionalService
        +isApplied(PriceCalculator, AdditionalService)$ boolean
    }
    class DeliveryDecorator {
        +calculate(OrderDraft) BigDecimal
    }
    class PackagingDecorator {
        +calculate(OrderDraft) BigDecimal
    }
    PriceCalculator <|.. BasePriceCalculator
    PriceCalculator <|.. PriceCalculatorDecorator
    PriceCalculatorDecorator <|-- DeliveryDecorator
    PriceCalculatorDecorator <|-- PackagingDecorator
    PriceCalculatorDecorator o--> PriceCalculator : delegate
    BasePriceCalculator o--> DiscountStrategy
    %% TODO: calculate в BasePriceCalculator, DeliveryDecorator, PackagingDecorator
```

Объекты для VIP-заказа с обеими услугами:

```mermaid
flowchart LR
    PK["PackagingDecorator<br/>+200"] --> DL["DeliveryDecorator<br/>+500"] --> B["BasePriceCalculator<br/>25 000 − 3 750"] --> V[VipDiscountStrategy]
```

Итог: `21 250 + 500 + 200 = 21 950`. Скидка рассчитывается только внутри `BasePriceCalculator`, поэтому на услуги не влияет.

## Сервис расчёта

```mermaid
classDiagram
    class OrderCalculationService {
        -discountStrategyFactory DiscountStrategyFactory
        -validationChain OrderValidationChain
        +calculate(OrderDraft) OrderCalculation
    }
    OrderCalculationService --> DiscountStrategyFactory
    OrderCalculationService --> OrderValidationChain
    OrderCalculationService ..> BasePriceCalculator : создаёт
    OrderCalculationService ..> PriceCalculatorDecorator : оборачивает
    OrderCalculationService ..> OrderCalculation : возвращает
    %% TODO: calculate
```

## Точки расширения

| Изменение | Что добавить | Что изменить |
|---|---|---|
| Новый тип скидки | Класс `implements DiscountStrategy` | Значение `CustomerType` и одна ветка в `DiscountStrategyFactory` |
| Новая проверка | Класс `extends AbstractOrderValidator` | Одна строка в `OrderValidationChain.createDefault()` |
| Новая услуга | Класс `extends PriceCalculatorDecorator` | Значение `AdditionalService` и одно сопоставление услуги и декоратора в сервисе |

Алгоритм `OrderCalculationService#calculate` при этом не меняется. Правка фабрики и места сборки — осознанная плата за Simple Factory
и ручную сборку; убрать её можно реестром стратегий/декораторов или DI-контейнером (список бинов `OrderValidator` с `@Order`).
