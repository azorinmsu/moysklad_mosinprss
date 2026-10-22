package ru.moysklad.intern.remapstub.seed;

import java.util.UUID;

/**
 * Стабильные идентификаторы тестовых данных заглушки Remap API.
 */
public final class RemapStubIds {

    public static final UUID ACCOUNT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    public static final UUID ORGANIZATION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID COUNTERPARTY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID STORE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    /** Товар с достаточным остатком (stock=100, reserve=0). */
    public static final UUID PRODUCT_SUFFICIENT = UUID.fromString("a1000000-0000-0000-0000-000000000001");
    /** Товар с нулевым остатком. */
    public static final UUID PRODUCT_ZERO = UUID.fromString("a1000000-0000-0000-0000-000000000002");
    /** Товар с частично зарезервированным остатком (stock=20, reserve=12 → available=8). */
    public static final UUID PRODUCT_PARTIAL = UUID.fromString("a1000000-0000-0000-0000-000000000003");

    /** Заказ с одной позицией (достаточно остатков). */
    public static final UUID ORDER_SINGLE = UUID.fromString("b2000000-0000-0000-0000-000000000001");
    public static final UUID ORDER_SINGLE_POS = UUID.fromString("b2100000-0000-0000-0000-000000000001");

    /** Заказ с несколькими позициями. */
    public static final UUID ORDER_MULTI = UUID.fromString("b2000000-0000-0000-0000-000000000002");
    public static final UUID ORDER_MULTI_POS_1 = UUID.fromString("b2100000-0000-0000-0000-000000000021");
    public static final UUID ORDER_MULTI_POS_2 = UUID.fromString("b2100000-0000-0000-0000-000000000022");

    /** Заказ с недостаточным остатком. */
    public static final UUID ORDER_INSUFFICIENT = UUID.fromString("b2000000-0000-0000-0000-000000000003");
    public static final UUID ORDER_INSUFFICIENT_POS = UUID.fromString("b2100000-0000-0000-0000-000000000031");

    /** Заказ с уже установленным резервом. */
    public static final UUID ORDER_WITH_RESERVE = UUID.fromString("b2000000-0000-0000-0000-000000000004");
    public static final UUID ORDER_WITH_RESERVE_POS = UUID.fromString("b2100000-0000-0000-0000-000000000041");

    /** Заказ, для которого уже существует отгрузка. */
    public static final UUID ORDER_WITH_DEMAND = UUID.fromString("b2000000-0000-0000-0000-000000000005");
    public static final UUID ORDER_WITH_DEMAND_POS = UUID.fromString("b2100000-0000-0000-0000-000000000051");
    public static final UUID DEMAND_EXISTING = UUID.fromString("c3000000-0000-0000-0000-000000000001");
    public static final UUID DEMAND_EXISTING_POS = UUID.fromString("c3100000-0000-0000-0000-000000000001");

    private RemapStubIds() {
    }
}
