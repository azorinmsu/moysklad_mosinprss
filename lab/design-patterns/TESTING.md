# Тестирование лабораторной «Паттерны проектирования»

Все тесты лабораторной — быстрые unit-тесты JUnit 5 без Spring Context, PostgreSQL, Kafka и Camunda.
Docker для них не нужен.

## Команды

Из каталога `intern/`:

```bash
# только тесты лабораторной
./mvnw test -Dtest='ru.moysklad.intern.patterns.**'

# один класс
./mvnw test -Dtest=DiscountStrategyTest

# один метод
./mvnw test -Dtest='OrderCalculationServiceTest#vipOrderFullCalculation'

# компиляция основного и тестового кода
./mvnw test-compile

# все тесты проекта (интеграционные требуют Docker для Testcontainers)
./mvnw test
```

В IntelliJ IDEA: правый клик по пакету `ru.moysklad.intern.patterns` в `src/test/java` → *Run Tests*.

## Что ожидать

| Этап | `PatternsInfrastructureTest` | Остальные тесты `patterns` |
|---|---|---|
| До начала работы | 8 проходят | 35 пропущены (`@Disabled`) |
| После выполнения | 8 проходят | 35 проходят, 0 пропущено |

`PatternsInfrastructureTest` проверяет готовую инфраструктуру (модели, фикстуры, связывание звеньев и обёрток).
Если он падает — сломана инфраструктура, а не ваше решение.

## Порядок работы с тестом-заготовкой

1. Удалите `@Disabled("TODO: implement test")`.
2. Замените `fail("TODO: implement test")` на проверки по подсказке в комментарии `TODO`.
3. Удалите комментарий `TODO`.
4. Запустите тест до реализации метода — он должен упасть (`UnsupportedOperationException` или неверное значение).
5. Реализуйте метод и убедитесь, что тест прошёл.

Тело-заглушка `fail(...)` гарантирует, что тест без проверок не станет зелёным, если просто снять `@Disabled`.

## Рекомендации по JUnit 5

- **Структура Arrange / Act / Assert.** Подготовка данных, одно действие, проверки.
- **`@DisplayName`** — бизнес-формулировка сценария на русском: «WHOLESALE на пороге: скидка 10%».
- **Готовые данные.** Используйте `OrderTestData` (`createVipOrder()`, `createOrderWithSubtotal(...)`, `item(...)`, `rub(...)`)
  вместо ручной сборки заказов в каждом тесте.
- **Деньги.** Ожидания задавайте литералами через `rub("3750")` и сравнивайте `isEqualTo` — так проверяется и масштаб `2`.
  Если масштаб не важен — `isEqualByComparingTo(...)`. Не вычисляйте ожидаемое значение тем же кодом, что проверяете.
- **Исключения.** Проверяйте тип и бизнес-код:

  ```java
  assertThatThrownBy(() -> chain.validate(order))
          .isInstanceOfSatisfying(OrderValidationException.class,
                  e -> assertThat(e.getCode()).isEqualTo(ValidationErrorCode.EMPTY_ORDER));
  ```

- **Отсутствие исключения:** `assertThatCode(() -> chain.validate(order)).doesNotThrowAnyException()`.
- **Параметризованные тесты** (`junit-jupiter-params` уже в зависимостях) удобны для таблиц «сумма → скидка»:

  ```java
  @ParameterizedTest
  @CsvSource({"49999.99, 0.00", "50000.00, 5000.00"})
  void wholesale(String subtotal, String expected) { ... }
  ```

- **Тестовый «шпион».** `RecordingOrderValidator` фиксирует, дошёл ли до него заказ. Поставьте его после проверяемого звена,
  чтобы доказать, что цепочка остановилась после ошибки.
- **Изоляция декораторов.** Подставляйте базовый калькулятор-лямбду `order -> rub("1000")` или `RegularDiscountStrategy`,
  чтобы тест декоратора не зависел от правил скидки.
- **Без Spring.** Не добавляйте `@SpringBootTest` в тесты пакета `patterns`: им не нужен контекст, а с ним они требуют Docker и запускаются в сотни раз медленнее.

## Самопроверка перед сдачей

```bash
cd intern
./mvnw test -Dtest='ru.moysklad.intern.patterns.**'
grep -rn "TODO" src/main/java/ru/moysklad/intern/patterns src/test/java/ru/moysklad/intern/patterns
grep -rn "@Disabled" src/test/java/ru/moysklad/intern/patterns
```

Ожидается: `Tests run: 43, Failures: 0, Errors: 0, Skipped: 0`, оба `grep` ничего не выводят.
