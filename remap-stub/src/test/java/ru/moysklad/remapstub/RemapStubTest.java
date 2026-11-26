package ru.moysklad.remapstub;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RemapStubTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    void reset() throws Exception {
        mvc.perform(post("/api/stub/reset")).andExpect(status().isOk());
    }

    @Test
    void catalogIsReadableAndProtocolIsEnforced() throws Exception {
        mvc.perform(get("/api/remap/1.2/entity/product"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get("/api/remap/1.2/entity/product").header("Accept-Encoding", "gzip"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/remap/1.2/entity/product").header("Authorization", "Bearer test"))
                .andExpect(status().isUnsupportedMediaType());

        JsonNode page = json(mvc.perform(remap(get("/api/remap/1.2/entity/product")))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(page.get("rows")).anyMatch(row -> "Футболка".equals(row.path("name").asText()));
    }

    @Test
    void reserveReducesFreeStockAndRejectsOverflow() throws Exception {
        String orderId = createOrder(new BigDecimal("5"), new BigDecimal("2"), "reserve-ok").get("id").asText();
        assertThat(freeStock(SeedIds.PRODUCT, SeedIds.STORE)).isEqualByComparingTo("8");

        JsonNode overflow = orderBody(new BigDecimal("2"), new BigDecimal("3"), "reserve-bad");
        mvc.perform(remap(post("/api/remap/1.2/entity/customerorder"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overflow.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).contains("17003"));

        JsonNode order = json(mvc.perform(remap(get("/api/remap/1.2/entity/customerorder/" + orderId + "?expand=positions")))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(order.get("positions").get("rows").get(0).path("reserve").decimalValue()).isEqualByComparingTo("2");
    }

    @Test
    void syncIdReturnsTheSameDocument() throws Exception {
        JsonNode first = createOrder(BigDecimal.ONE, BigDecimal.ONE, "same-sync");
        JsonNode second = createOrder(new BigDecimal("9"), BigDecimal.ZERO, "same-sync");
        assertThat(second.get("id").asText()).isEqualTo(first.get("id").asText());
        assertThat(freeStock(SeedIds.PRODUCT, SeedIds.STORE)).isEqualByComparingTo("9");
    }

    @Test
    void shipmentReturnAndPaymentMoveStockAndMoney() throws Exception {
        JsonNode order = createOrder(new BigDecimal("5"), new BigDecimal("2"), "flow");
        JsonNode demand = json(mvc.perform(remap(post("/api/remap/1.2/entity/demand"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(demandBody(order.get("id").asText(), BigDecimal.ONE).toString()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(demand.path("applicable").asBoolean()).isTrue();
        assertThat(physical(SeedIds.PRODUCT, SeedIds.STORE)).isEqualByComparingTo("9");
        assertThat(freeStock(SeedIds.PRODUCT, SeedIds.STORE)).isEqualByComparingTo("8");

        JsonNode reread = json(mvc.perform(remap(get("/api/remap/1.2/entity/customerorder/" + order.get("id").asText() + "?expand=positions")))
                .andReturn());
        JsonNode position = reread.get("positions").get("rows").get(0);
        assertThat(position.path("shipped").decimalValue()).isEqualByComparingTo("1");
        assertThat(position.path("reserve").decimalValue()).isEqualByComparingTo("1");

        mvc.perform(remap(post("/api/remap/1.2/entity/salesreturn"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(returnBody(demand.get("id").asText(), BigDecimal.ONE).toString()))
                .andExpect(status().isOk());
        assertThat(physical(SeedIds.PRODUCT, SeedIds.STORE)).isEqualByComparingTo("10");

        BigDecimal beforePay = physical(SeedIds.PRODUCT, SeedIds.STORE);
        mvc.perform(remap(post("/api/remap/1.2/entity/paymentin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody(order.get("id").asText(), new BigDecimal("150000")).toString()))
                .andExpect(status().isOk());
        JsonNode paid = json(mvc.perform(remap(get("/api/remap/1.2/entity/customerorder/" + order.get("id").asText()))).andReturn());
        assertThat(paid.path("payedSum").decimalValue()).isEqualByComparingTo("150000");
        assertThat(physical(SeedIds.PRODUCT, SeedIds.STORE)).isEqualByComparingTo(beforePay);
    }

    @Test
    void demandTemplateIsNotPersisted() throws Exception {
        JsonNode order = createOrder(BigDecimal.ONE, BigDecimal.ZERO, "template");
        ObjectNode body = mapper.createObjectNode();
        body.set("customerOrder", ref(order.get("id").asText(), "customerorder"));
        JsonNode template = json(mvc.perform(remap(put("/api/remap/1.2/entity/demand/new"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(template.has("id")).isFalse();
        JsonNode demands = json(mvc.perform(remap(get("/api/remap/1.2/entity/demand"))).andReturn());
        assertThat(demands.get("meta").path("size").asInt()).isZero();
    }

    @Test
    void lostResponseStillStoresDocument() throws Exception {
        ObjectNode fault = mapper.createObjectNode();
        fault.put("method", "POST");
        fault.put("pathPattern", "/api/remap/1.2/entity/customerorder");
        fault.put("faultType", "LOSE_RESPONSE");
        fault.put("count", 1);
        mvc.perform(post("/api/stub/faults").contentType(MediaType.APPLICATION_JSON).content(fault.toString()))
                .andExpect(status().isOk());

        MvcResult lost = mvc.perform(remap(post("/api/remap/1.2/entity/customerorder"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(BigDecimal.ONE, BigDecimal.ZERO, "lost-sync").toString()))
                .andReturn();
        assertThat(lost.getResponse().getContentAsString()).isBlank();

        JsonNode found = json(mvc.perform(remap(get("/api/remap/1.2/entity/customerorder?filter=syncId=lost-sync"))).andReturn());
        assertThat(found.get("rows")).hasSize(1);
    }

    @Test
    void webhookIsRecordedAndCanBeRedelivered() throws Exception {
        ObjectNode subscription = mapper.createObjectNode();
        subscription.put("url", "http://127.0.0.1:9/hook");
        subscription.put("action", "CREATE");
        subscription.put("entityType", "customerorder");
        mvc.perform(remap(post("/api/remap/1.2/entity/webhook"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(subscription.toString()))
                .andExpect(status().isOk());
        createOrder(BigDecimal.ONE, BigDecimal.ZERO, "hook-order");
        JsonNode deliveries = json(mvc.perform(get("/api/stub/webhook-deliveries")).andReturn());
        assertThat(deliveries).isNotEmpty();
        String requestId = deliveries.get(0).path("requestId").asText();
        ObjectNode again = mapper.createObjectNode();
        again.put("deliveryId", deliveries.get(0).path("id").asText());
        mvc.perform(post("/api/stub/webhooks/redeliver").contentType(MediaType.APPLICATION_JSON).content(again.toString()))
                .andExpect(status().isOk());
        JsonNode after = json(mvc.perform(get("/api/stub/webhook-deliveries")).andReturn());
        assertThat(after).anyMatch(item -> requestId.equals(item.path("requestId").asText()) && !item.path("id").asText().equals(deliveries.get(0).path("id").asText()));
    }

    @Test
    void resetRestoresSeedStock() throws Exception {
        createOrder(BigDecimal.ONE, BigDecimal.ONE, "to-reset");
        mvc.perform(post("/api/stub/reset")).andExpect(status().isOk());
        assertThat(freeStock(SeedIds.PRODUCT, SeedIds.STORE)).isEqualByComparingTo("10");
        mvc.perform(delete("/api/stub/faults")).andExpect(status().isOk());
    }

    private JsonNode createOrder(BigDecimal quantity, BigDecimal reserve, String syncId) throws Exception {
        return json(mvc.perform(remap(post("/api/remap/1.2/entity/customerorder"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(quantity, reserve, syncId).toString()))
                .andExpect(status().isOk())
                .andReturn());
    }

    private ObjectNode orderBody(BigDecimal quantity, BigDecimal reserve, String syncId) {
        ObjectNode order = mapper.createObjectNode();
        order.put("syncId", syncId);
        order.set("organization", ref(SeedIds.ORGANIZATION, "organization"));
        order.set("agent", ref(SeedIds.COUNTERPARTY, "counterparty"));
        order.set("store", ref(SeedIds.STORE, "store"));
        order.put("applicable", true);
        ArrayNode positions = mapper.createArrayNode();
        ObjectNode position = mapper.createObjectNode();
        position.set("assortment", ref(SeedIds.PRODUCT, "product"));
        position.put("quantity", quantity);
        position.put("reserve", reserve);
        position.put("price", 150000);
        positions.add(position);
        order.set("positions", positions);
        return order;
    }

    private ObjectNode demandBody(String orderId, BigDecimal quantity) {
        ObjectNode demand = mapper.createObjectNode();
        demand.set("organization", ref(SeedIds.ORGANIZATION, "organization"));
        demand.set("agent", ref(SeedIds.COUNTERPARTY, "counterparty"));
        demand.set("store", ref(SeedIds.STORE, "store"));
        demand.set("customerOrder", ref(orderId, "customerorder"));
        demand.put("applicable", true);
        ArrayNode positions = mapper.createArrayNode();
        ObjectNode position = mapper.createObjectNode();
        position.set("assortment", ref(SeedIds.PRODUCT, "product"));
        position.put("quantity", quantity);
        position.put("price", 150000);
        positions.add(position);
        demand.set("positions", positions);
        return demand;
    }

    private ObjectNode returnBody(String demandId, BigDecimal quantity) {
        ObjectNode salesReturn = mapper.createObjectNode();
        salesReturn.set("organization", ref(SeedIds.ORGANIZATION, "organization"));
        salesReturn.set("agent", ref(SeedIds.COUNTERPARTY, "counterparty"));
        salesReturn.set("store", ref(SeedIds.STORE, "store"));
        salesReturn.set("demand", ref(demandId, "demand"));
        salesReturn.put("applicable", true);
        ArrayNode positions = mapper.createArrayNode();
        ObjectNode position = mapper.createObjectNode();
        position.set("assortment", ref(SeedIds.PRODUCT, "product"));
        position.put("quantity", quantity);
        position.put("price", 150000);
        positions.add(position);
        salesReturn.set("positions", positions);
        return salesReturn;
    }

    private ObjectNode paymentBody(String orderId, BigDecimal sum) {
        ObjectNode payment = mapper.createObjectNode();
        payment.set("organization", ref(SeedIds.ORGANIZATION, "organization"));
        payment.set("agent", ref(SeedIds.COUNTERPARTY, "counterparty"));
        payment.put("sum", sum);
        payment.put("applicable", true);
        ArrayNode operations = mapper.createArrayNode();
        ObjectNode operation = ref(orderId, "customerorder");
        operation.put("linkedSum", sum);
        operations.add(operation);
        payment.set("operations", operations);
        return payment;
    }

    private ObjectNode ref(String id, String type) {
        ObjectNode ref = mapper.createObjectNode();
        ObjectNode meta = mapper.createObjectNode();
        meta.put("href", "http://localhost:8088/api/remap/1.2/entity/" + type + "/" + id);
        meta.put("type", type);
        meta.put("mediaType", "application/json");
        ref.set("meta", meta);
        return ref;
    }

    private BigDecimal freeStock(String assortmentId, String storeId) throws Exception {
        return current(assortmentId, storeId, "freeStock");
    }

    private BigDecimal physical(String assortmentId, String storeId) throws Exception {
        return current(assortmentId, storeId, "stock");
    }

    private BigDecimal current(String assortmentId, String storeId, String field) throws Exception {
        JsonNode rows = json(mvc.perform(remap(get("/api/remap/1.2/report/stock/bystore/current")
                .param("stockType", field)
                .param("include", "zeroLines"))).andReturn());
        for (JsonNode row : rows) {
            if (assortmentId.equals(row.path("assortmentId").asText()) && storeId.equals(row.path("storeId").asText())) {
                return row.path(field).decimalValue();
            }
        }
        throw new AssertionError("Остаток не найден для " + assortmentId);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder remap(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer test").header("Accept-Encoding", "gzip");
    }
}
