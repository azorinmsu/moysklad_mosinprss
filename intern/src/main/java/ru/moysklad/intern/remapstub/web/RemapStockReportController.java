package ru.moysklad.intern.remapstub.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.moysklad.intern.remapstub.service.RemapStockService;

import java.util.Map;

@RestController
@RequestMapping("/api/remap/1.2/report/stock")
@ConditionalOnProperty(prefix = "app.remap-stub", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RemapStockReportController {

    private final RemapStockService stockService;

    public RemapStockReportController(RemapStockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/all")
    public Map<String, Object> all() {
        return stockService.reportAll();
    }
}
