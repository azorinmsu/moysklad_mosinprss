# Лабораторная работа: паттерны проектирования в ERP-системе МойСклад

**Тема:** применение паттернов проектирования при расчёте и оформлении заказа покупателя.

Сопутствующие документы:

- [ARCHITECTURE.md](ARCHITECTURE.md) — UML-диаграммы (Mermaid) и разбор архитектуры;
- [TESTING.md](TESTING.md) — запуск тестов и рекомендации по JUnit 5.

---

## Бизнес-сценарий

Покупатель оформляет в МойСклад заказ на товары. Система должна:

1. Получить список товаров и их цены.
2. Рассчитать базовую стоимость (стоимость товаров).
3. Применить подходящую скидку.
4. Проверить бизнес-ограничения заказа.
5. Применить дополнительные платные услуги.
6. Сформировать итоговый расчёт.

Покупатели бывают разных типов, условия оформления заказов различаются.
Архитектура должна позволять добавлять новые скидки, проверки и услуги **без изменения центрального алгоритма расчёта**.

Это учебная модель: здесь нет REST, БД, Kafka и Camunda. Весь код — обычные Java-классы, тесты — быстрые unit-тесты без Spring Context.
Реальные API МойСклад не используются.

## Где лежит код

```text
intern/src/main/java/ru/moysklad/intern/patterns/
├── model/        CustomerType, AdditionalService, Money, OrderItem, OrderDraft, OrderCalculation  (готово)
├── discount/     DiscountStrategy (готово), Regular/Wholesale/VipDiscountStrategy, DiscountStrategyFactory (TODO),
│                 UnsupportedCustomerTypeException (готово)
├── validation/   OrderValidator, AbstractOrderValidator, OrderValidationException, ValidationErrorCode (готово),
│                 EmptyOrder/Quantity/Price/TotalAmountValidator (TODO), OrderValidationChain (createDefault — TODO)
├── pricing/      PriceCalculator, PriceCalculatorDecorator (готово),
│                 BasePriceCalculator, DeliveryDecorator, PackagingDecorator (TODO)
└── service/      OrderCalculationService (calculate — TODO)

intern/src/test/java/ru/moysklad/intern/patterns/
├── OrderTestData.java               готовые фабрики тестовых данных
├── RecordingOrderValidator.java     тестовое звено цепочки («шпион»)
├── PatternsInfrastructureTest.java  проверки готовой инфраструктуры (включены, проходят сразу)
├── DiscountStrategyTest.java        TODO
├── DiscountStrategyFactoryTest.java TODO
├── OrderValidationChainTest.java    TODO
├── PriceCalculatorDecoratorTest.java TODO
└── OrderCalculationServiceTest.java TODO
```

### Готовая инфраструктура (менять не нужно)

| Класс | Что даёт |
|---|---|
| `Money` | Единое правило денег: 2 знака, `RoundingMode.HALF_UP`; `Money.round(...)`, `Money.of("500")`, `Money.ZERO` |
| `CustomerType` | `REGULAR`, `WHOLESALE`, `VIP` и `CORPORATE` — тип без утверждённой скидки (для проверки «неподдерживаемого типа») |
| `AdditionalService` | `DELIVERY` (500 ₽), `GIFT_PACKAGING` (200 ₽) — единственный источник цен услуг |
| `OrderItem` | Позиция: товар, цена, количество; `lineTotal()`; `fromProduct(Product, qty)` — мост к каталожному `Product` |
| `OrderDraft` | Черновик заказа: тип покупателя, позиции, множество услуг; `subtotal()`, `hasService(...)` |
| `OrderCalculation` | Результат: `subtotal`, `discount`, `subtotalAfterDiscount`, `deliveryCost`, `packagingCost`, `total` |
| `AbstractOrderValidator` | Звено цепочки: `linkWith(next)` и `passToNext(order)` |
| `OrderValidationChain` | Конструктор и `validate(order)` — вход в цепочку |
| `PriceCalculatorDecorator` | Хранит `delegate` и `service`; запрещает повторно обернуть одну услугу |
| `OrderTestData` | Фикстуры: `createRegularOrder()`, `createWholesaleOrder()`, `createVipOrder()`, `createEmptyOrder()`, `createOrderWithInvalidQuantity()` и др. |

### Общие правила

- Денежные суммы — только `BigDecimal`. `double`/`float` запрещены.
- Сравнение `BigDecimal` по значению — `compareTo`, не `equals` (`equals` учитывает масштаб: `5.0 ≠ 5.00`).
- Каждый денежный результат проходит через `Money.round(...)`.
- Нельзя менять сигнатуры публичных методов-заготовок и готовую инфраструктуру.
- Все методы с `throw new UnsupportedOperationException("TODO")` должны быть реализованы, а комментарии `TODO` удалены.
- В тестах снимите `@Disabled` и замените `fail("TODO: implement test")` реальными проверками.

### Список методов с `TODO`

| Задание | Класс | Метод |
|---|---|---|
| 1 | `RegularDiscountStrategy` | `calculateDiscount(BigDecimal)` |
| 1 | `WholesaleDiscountStrategy` | `calculateDiscount(BigDecimal)` |
| 1 | `VipDiscountStrategy` | `calculateDiscount(BigDecimal)` |
| 2 | `DiscountStrategyFactory` | `create(CustomerType)` |
| 3 | `EmptyOrderValidator` | `validate(OrderDraft)` |
| 3 | `QuantityValidator` | `validate(OrderDraft)` |
| 3 | `PriceValidator` | `validate(OrderDraft)` |
| 3 | `TotalAmountValidator` | `validate(OrderDraft)` |
| 3 | `OrderValidationChain` | `static createDefault()` |
| 4 | `BasePriceCalculator` | `calculate(OrderDraft)` |
| 4 | `DeliveryDecorator` | `calculate(OrderDraft)` |
| 4 | `PackagingDecorator` | `calculate(OrderDraft)` |
| 5 | `OrderCalculationService` | `calculate(OrderDraft)` |
| все | `*Test` в `patterns` | все методы с `@Disabled("TODO: implement test")` |

---

## Задание 1. Strategy — расчёт скидок (20 баллов)

**Цель.** Вынести каждое правило скидки в отдельный взаимозаменяемый класс.

**Паттерн.** Strategy определяет семейство алгоритмов, инкапсулирует каждый и делает их взаимозаменяемыми.
Клиент работает с интерфейсом `DiscountStrategy` и не знает, какая реализация подставлена.

**Проблема.** Без паттерна правила скидок превращаются в большой `if/switch` по типу покупателя внутри сервиса расчёта.
Каждая новая акция требует правки центрального метода, растёт риск сломать соседние правила, их нельзя протестировать по отдельности.

**Где в ERP.** Скидки по типу контрагента, по сумме заказа, накопительные, сезонные акции, скидки по карте лояльности.

### Бизнес-правила

| Тип | Правило |
|---|---|
| `REGULAR` | Скидка 0% |
| `WHOLESALE` | Скидка 10%, если сумма товаров **от** 50 000 ₽ (включительно) |
| `VIP` | Скидка 15% независимо от суммы |

Скидка применяется к стоимости товаров **до** добавления доставки и упаковки.

### Что реализовать

- `RegularDiscountStrategy#calculateDiscount`
- `WholesaleDiscountStrategy#calculateDiscount` (константы `THRESHOLD`, `RATE` уже объявлены)
- `VipDiscountStrategy#calculateDiscount` (константа `RATE` уже объявлена)

### Контракт (Javadoc `DiscountStrategy`)

- `subtotal == null` или `subtotal < 0` → `IllegalArgumentException`;
- результат — **сумма** скидки в рублях, а не процент;
- `0.00 <= скидка <= subtotal`;
- результат округлён через `Money.round`.

### Как подойти к решению

1. Проверка входа одинакова для всех стратегий. Подумайте, где её разместить, чтобы не копировать в трёх классах
   (например, небольшой package-private помощник или абстрактный базовый класс в пакете `discount`). Сигнатуры интерфейса менять нельзя.
2. Для оптовой скидки граница включительная: «от 50 000» означает `subtotal >= THRESHOLD`.
3. Округляйте **результат** умножения, а не ставку.

### Тесты (`DiscountStrategyTest`)

Обычный покупатель; оптовый ниже порога, на пороге, выше порога; VIP; нулевая стоимость; округление (`333.33 × 15% = 49.9995 → 50.00`,
`50 000.05 × 10% = 5 000.005 → 5 000.01`); границы скидки; некорректный вход.

**Ожидаемый результат.** Все 9 тестов включены и проходят.

---

## Задание 2. Фабрика стратегий (15 баллов)

**Цель.** Сделать фабрику единственным местом, где выбирается конкретная стратегия по типу покупателя.

**Паттерн.** В заготовке используется **Simple Factory** (параметризованная фабрика): один метод `create(CustomerType)` возвращает
нужную реализацию `DiscountStrategy`. Это упрощённый учебный вариант. Он **не является** GoF Factory Method — разница описана
в [ARCHITECTURE.md](ARCHITECTURE.md#factory-method-и-simple-factory).

**Проблема.** Если сервис расчёта сам пишет `new VipDiscountStrategy()`, он зависит от всех конкретных классов,
и при добавлении нового типа покупателя придётся менять бизнес-сервис.

**Где в ERP.** Выбор стратегии скидки, способа расчёта налога (НДС/без НДС), алгоритма списания себестоимости (FIFO/средняя), формата печатной формы.

### Что реализовать

`DiscountStrategyFactory#create(CustomerType)`:

- `null` → `IllegalArgumentException`;
- `REGULAR`, `WHOLESALE`, `VIP` → соответствующая стратегия;
- `CORPORATE` (и любой будущий тип без стратегии) → `UnsupportedCustomerTypeException`.

### Как подойти к решению

- Удобно использовать `switch`-выражение по `enum`. Подумайте, как явно обработать тип без стратегии.
- Стратегии без состояния можно создавать один раз и переиспользовать.
- Альтернатива — `EnumMap<CustomerType, DiscountStrategy>` (реестр). Сравните, что меняется при добавлении нового типа в каждом случае.
- В `OrderCalculationService` **не должно быть** выбора стратегии по типу.

### Тесты (`DiscountStrategyFactoryTest`)

`REGULAR`, `WHOLESALE`, `VIP` — проверить класс возвращённой стратегии; `CORPORATE` — `UnsupportedCustomerTypeException`
с правильным `getCustomerType()`; `null` — `IllegalArgumentException`.

**Ожидаемый результат.** Все 5 тестов включены и проходят.

---

## Задание 3. Chain of Responsibility — проверки заказа (20 баллов)

**Цель.** Разложить проверки заказа на независимые звенья с явным порядком.

**Паттерн.** Chain of Responsibility передаёт запрос по цепочке обработчиков. Каждый обработчик выполняет свою часть
и **сам решает**, передавать ли запрос дальше. Клиент знает только первое звено.

**Проблема.** Метод `validate()` на сотню строк со всеми правилами сразу: сложно читать, нельзя переиспользовать
отдельные правила, нельзя собрать другой набор проверок для другого сценария (например, для возврата).

**Где в ERP.** Проверки документа перед проведением: заполненность, остатки, лимит кредита контрагента, права пользователя, закрытый период.

### Правила и порядок

| № | Звено | Правило | Код ошибки |
|---|---|---|---|
| 1 | `EmptyOrderValidator` | В заказе есть хотя бы одна позиция | `EMPTY_ORDER` |
| 2 | `QuantityValidator` | Количество каждой позиции задано и `> 0` | `INVALID_QUANTITY` |
| 3 | `PriceValidator` | Цена каждой позиции задана и `>= 0` | `INVALID_PRICE` |
| 4 | `TotalAmountValidator` | `subtotal <= 1 000 000,00 ₽` | `TOTAL_AMOUNT_EXCEEDED` |

При первой ошибке выбрасывается `OrderValidationException` с кодом; следующие звенья **не выполняются**.

### Что реализовать

- `validate(OrderDraft)` в четырёх валидаторах;
- `OrderValidationChain.createDefault()` — сборка цепочки в указанном порядке.

### Как подойти к решению

- Каждое звено наследует `AbstractOrderValidator`: проверяет **одно** правило, при нарушении бросает исключение,
  иначе вызывает `passToNext(order)`. Не забудьте вызвать `passToNext` — иначе цепочка оборвётся на успешной проверке.
- `linkWith` возвращает переданное звено, поэтому цепочку можно собрать одним выражением.
- `TotalAmountValidator` использует `OrderDraft#subtotal()`; он стоит последним и полагается на то, что цены и количества уже проверены.
- В сообщение ошибки по позиции включайте наименование товара — так пользователь ERP поймёт, что исправить.

### Тесты (`OrderValidationChainTest`)

Пустой заказ; нулевое количество; отрицательное количество; отрицательная цена; превышение максимума и граница 1 000 000,00;
корректные заказы; прекращение цепочки после ошибки (через `RecordingOrderValidator`); порядок звеньев.

**Ожидаемый результат.** Все 8 тестов включены и проходят.

---

## Задание 4. Decorator — дополнительные услуги (20 баллов)

**Цель.** Добавлять платные услуги к расчёту, не меняя базовый калькулятор и не создавая подкласс на каждую комбинацию.

**Паттерн.** Decorator динамически добавляет объекту поведение, оборачивая его в объект того же интерфейса.
Обёртки можно вкладывать друг в друга в любом количестве.

**Проблема.** Через наследование для двух услуг понадобились бы классы «с доставкой», «с упаковкой», «с доставкой и упаковкой»;
для трёх услуг — уже 7 комбинаций. Через `if` в калькуляторе — снова центральный метод, который правится при каждой новой услуге.

**Где в ERP.** Доставка, упаковка, страховка груза, сборка, срочная обработка, комиссия платёжной системы.

### Правила

- Доставка — 500 ₽ (`AdditionalService.DELIVERY`), подарочная упаковка — 200 ₽ (`AdditionalService.GIFT_PACKAGING`).
- Услуги применяются независимо и одновременно.
- `BasePriceCalculator` возвращает стоимость товаров **после скидки**; стратегия скидки передаётся через конструктор.
- Скидка **не применяется** к услугам.
- Одна услуга не может быть начислена дважды: `PriceCalculatorDecorator` уже бросает `IllegalStateException` при повторной обёртке.

### Что реализовать

- `BasePriceCalculator#calculate`
- `DeliveryDecorator#calculate`
- `PackagingDecorator#calculate`

### Как подойти к решению

- Базовый калькулятор: `subtotal` заказа минус скидка стратегии.
- Декоратор: результат `delegate.calculate(order)` плюс `service.price()`. Декоратор не знает, что внутри — базовый калькулятор или другой декоратор.
- Декоратор добавляет **только свою** стоимость и не проверяет `order.hasService(...)`: решение, какие обёртки нужны, принимает тот, кто собирает калькулятор (задание 5).

### Тесты (`PriceCalculatorDecoratorTest`)

Без услуг; только доставка; только упаковка; доставка + упаковка; независимость от порядка обёрток; скидка не затрагивает услуги;
запрет повторной услуги.

**Ожидаемый результат.** Все 7 тестов включены и проходят.

---

## Задание 5. Объединение паттернов (15 баллов)

**Цель.** Собрать сервис расчёта, который использует все паттерны и не зависит от конкретных стратегий, правил и услуг.

### Что реализовать

`OrderCalculationService#calculate(OrderDraft)`:

1. Выполнить цепочку проверок (`validationChain`).
2. Получить стратегию скидки из фабрики.
3. Создать `BasePriceCalculator` со стратегией.
4. Обернуть его декораторами для услуг из `order.additionalServices()`.
5. Рассчитать итоговую стоимость калькулятором.
6. Вернуть `OrderCalculation` со всеми составляющими.

### Как подойти к решению

- Сначала проверка, потом расчёт: некорректный заказ не должен доходить до скидок.
- Сборку обёрток вынесите в отдельный приватный метод. Подумайте, как сопоставить `AdditionalService` и конструктор декоратора,
  чтобы новая услуга добавлялась одной строкой (например, `EnumMap<AdditionalService, UnaryOperator<PriceCalculator>>`).
- `deliveryCost`/`packagingCost` равны цене услуги, если она выбрана, иначе `0.00`.
- `total` берите из декорированного калькулятора и убедитесь (тестом), что он равен `subtotalAfterDiscount + deliveryCost + packagingCost`.

### Пример расчёта

VIP-покупатель: товар А 2 × 10 000 ₽, товар Б 1 × 5 000 ₽, доставка и упаковка (`OrderTestData.createVipOrder()`).

| Показатель | Сумма |
|---|---:|
| Стоимость товаров | 25 000,00 ₽ |
| Скидка 15% | 3 750,00 ₽ |
| После скидки | 21 250,00 ₽ |
| Доставка | 500,00 ₽ |
| Упаковка | 200,00 ₽ |
| **Итого** | **21 950,00 ₽** |

### Тесты (`OrderCalculationServiceTest`)

Пример VIP; REGULAR без услуг; WHOLESALE с доставкой (55 000 − 5 500 + 500 = 50 000); некорректный заказ; неподдерживаемый
тип покупателя; согласованность составляющих.

**Ожидаемый результат.** Все 6 тестов включены и проходят.

---

## Тесты и качество кода (10 баллов)

- Все тесты в `ru.moysklad.intern.patterns` включены, ни одного `@Disabled` и `fail("TODO ...")`.
- В основном коде не осталось `UnsupportedOperationException("TODO")` и комментариев `TODO`.
- Нет `double` для денег, суммы округлены единообразно.
- В каждом тесте одна проверяемая идея, понятный `@DisplayName`, явные ожидаемые значения (не пересчитанные тем же кодом, что и в решении).
- Существующие тесты проекта не сломаны.

## Оценивание

| Задание | Баллы |
|---|---:|
| Strategy | 20 |
| Фабрика стратегий | 15 |
| Chain of Responsibility | 20 |
| Decorator | 20 |
| Общий сервис расчёта | 15 |
| Тесты и качество кода | 10 |
| **Всего** | **100** |

## Вопросы для защиты

1. Чем Strategy отличается от большого `switch`?
2. Почему создание стратегий вынесено в фабрику?
3. В чём отличие Factory Method от Simple Factory?
4. Чем Chain of Responsibility отличается от обычного последовательного вызова методов?
5. Почему дополнительные услуги реализованы через Decorator?
6. Чем Decorator отличается от наследования?
7. Какие паттерны упрощают расширение ERP-системы?
8. Где нарушается Open/Closed Principle?
9. Как добавить новый тип скидки?
10. Как добавить новый валидатор заказа?
