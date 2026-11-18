package ru.moysklad.concurrency.web;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("baseline")
@Timeout(30)
class ErpApplicationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void applicationServesStockReservationAndPrice() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/api/reports/stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productId == 'paper-a4' && @.warehouseId == 'main')].quantity")
                        .value(100));
        mvc.perform(post("/api/reservations")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"orderId":"SO-1","productId":"paper-a4","warehouseId":"main","quantity":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true));
        mvc.perform(put("/api/prices/paper-a4")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"salePrice":15.00,"discountPercent":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value("paper-a4"));
        mvc.perform(post("/api/purchase-orders")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"supplierId":"supplier-1","productId":"pen-blue","warehouseId":"main","quantity":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }
}
