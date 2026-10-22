package ru.moysklad.intern.remapstub.service;

import org.springframework.stereotype.Component;
import ru.moysklad.intern.remapstub.entity.StubCustomerOrder;
import ru.moysklad.intern.remapstub.entity.StubDemand;
import ru.moysklad.intern.remapstub.entity.StubDemandPosition;
import ru.moysklad.intern.remapstub.entity.StubOrderPosition;
import ru.moysklad.intern.remapstub.entity.StubProduct;
import ru.moysklad.intern.remapstub.entity.StubStock;
import ru.moysklad.intern.remapstub.seed.RemapStubIds;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RemapJsonMapper {

    private final RemapMetaFactory meta;

    public RemapJsonMapper(RemapMetaFactory meta) {
        this.meta = meta;
    }

    public Map<String, Object> product(StubProduct product) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("meta", meta.entityMeta("product", product.getId()));
        body.put("id", product.getId().toString());
        body.put("accountId", meta.accountId());
        body.put("name", product.getName());
        if (product.getCode() != null) {
            body.put("code", product.getCode());
        }
        if (product.getDescription() != null) {
            body.put("description", product.getDescription());
        }
        body.put("archived", product.isArchived());
        return body;
    }

    public Map<String, Object> orderPosition(StubOrderPosition position, StubStock stock, boolean includeStock) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("meta", meta.entityMeta("customerorderposition", position.getId()));
        body.put("id", position.getId().toString());
        body.put("accountId", meta.accountId());
        body.put("quantity", position.getQuantity().doubleValue());
        body.put("price", position.getPrice());
        body.put("discount", 0.0);
        body.put("vat", 0);
        body.put("vatEnabled", false);
        body.put("reserve", position.getReserve().doubleValue());
        body.put("shipped", position.getShipped().doubleValue());
        body.put("assortment", meta.ref("product", position.getProductId()));
        if (includeStock && stock != null) {
            body.put("stock", meta.stockBlock(stock.getQuantity(), stock.getReserve(), stock.getInTransit()));
        }
        return body;
    }

    public Map<String, Object> customerOrder(
            StubCustomerOrder order,
            List<StubOrderPosition> positions,
            Map<java.util.UUID, StubStock> stocksByProduct,
            boolean expandPositions,
            boolean includeStock
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("meta", meta.entityMeta("customerorder", order.getId()));
        body.put("id", order.getId().toString());
        body.put("accountId", meta.accountId());
        body.put("name", order.getName());
        if (order.getDescription() != null) {
            body.put("description", order.getDescription());
        }
        body.put("applicable", order.isApplicable());
        body.put("moment", meta.formatMoment(order.getMoment()));
        body.put("organization", meta.namedRef("organization", order.getOrganizationId(), "Учебная организация"));
        body.put("agent", meta.namedRef("counterparty", order.getAgentId(), "Учебный контрагент"));
        body.put("store", meta.namedRef("store", order.getStoreId(), "Основной склад"));

        String positionsPath = "/entity/customerorder/" + order.getId() + "/positions";
        if (expandPositions) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (StubOrderPosition position : positions) {
                rows.add(orderPosition(position, stocksByProduct.get(position.getProductId()), includeStock));
            }
            body.put("positions", meta.listEntity("customerorderposition", positionsPath, rows));
        } else {
            Map<String, Object> positionsMeta = new LinkedHashMap<>();
            positionsMeta.put("meta", meta.collectionMeta("customerorderposition", positionsPath, positions.size()));
            body.put("positions", positionsMeta);
        }
        return body;
    }

    public Map<String, Object> demandPosition(StubDemandPosition position) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("meta", meta.entityMeta("demandposition", position.getId()));
        body.put("id", position.getId().toString());
        body.put("accountId", meta.accountId());
        body.put("quantity", position.getQuantity().doubleValue());
        body.put("price", position.getPrice());
        body.put("discount", 0.0);
        body.put("vat", 0);
        body.put("vatEnabled", false);
        body.put("assortment", meta.ref("product", position.getProductId()));
        return body;
    }

    public Map<String, Object> demand(
            StubDemand demand,
            List<StubDemandPosition> positions,
            boolean expandPositions
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("meta", meta.entityMeta("demand", demand.getId()));
        body.put("id", demand.getId().toString());
        body.put("accountId", meta.accountId());
        body.put("name", demand.getName());
        if (demand.getDescription() != null) {
            body.put("description", demand.getDescription());
        }
        body.put("applicable", demand.isApplicable());
        body.put("moment", meta.formatMoment(demand.getMoment()));
        body.put("organization", meta.namedRef("organization", demand.getOrganizationId(), "Учебная организация"));
        body.put("agent", meta.namedRef("counterparty", demand.getAgentId(), "Учебный контрагент"));
        body.put("store", meta.namedRef("store", demand.getStoreId(), "Основной склад"));
        if (demand.getCustomerOrderId() != null) {
            body.put("customerOrder", meta.ref("customerorder", demand.getCustomerOrderId()));
        }

        String positionsPath = "/entity/demand/" + demand.getId() + "/positions";
        if (expandPositions) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (StubDemandPosition position : positions) {
                rows.add(demandPosition(position));
            }
            body.put("positions", meta.listEntity("demandposition", positionsPath, rows));
        } else {
            Map<String, Object> positionsMeta = new LinkedHashMap<>();
            positionsMeta.put("meta", meta.collectionMeta("demandposition", positionsPath, positions.size()));
            body.put("positions", positionsMeta);
        }
        return body;
    }

    public Map<String, Object> stockReportRow(StubProduct product, StubStock stock) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("meta", meta.entityMeta("product", product.getId()));
        body.put("stock", stock.getQuantity().doubleValue());
        body.put("reserve", stock.getReserve().doubleValue());
        body.put("inTransit", stock.getInTransit().doubleValue());
        body.put("quantity", stock.available().doubleValue());
        body.put("name", product.getName());
        body.put("code", product.getCode());
        body.put("article", product.getCode());
        body.put("folder", Map.of(
                "meta", meta.entityMeta("productfolder", RemapStubIds.ACCOUNT_ID),
                "name", "Товары"
        ));
        return body;
    }
}
