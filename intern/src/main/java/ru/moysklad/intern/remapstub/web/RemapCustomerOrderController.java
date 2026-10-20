package ru.moysklad.intern.remapstub.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.moysklad.intern.remapstub.service.RemapCustomerOrderService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/remap/1.2/entity/customerorder")
@ConditionalOnProperty(prefix = "app.remap-stub", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RemapCustomerOrderController {

    private final RemapCustomerOrderService customerOrderService;

    public RemapCustomerOrderController(RemapCustomerOrderService customerOrderService) {
        this.customerOrderService = customerOrderService;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(
            @PathVariable UUID id,
            @RequestParam(required = false) String expand,
            @RequestParam(required = false) String fields
    ) {
        return customerOrderService.getById(
                id,
                RemapQuerySupport.expandPositions(expand),
                RemapQuerySupport.includeStock(fields)
        );
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        return customerOrderService.updateOrder(id, body);
    }

    @GetMapping("/{id}/positions")
    public Map<String, Object> getPositions(
            @PathVariable UUID id,
            @RequestParam(required = false) String fields
    ) {
        return customerOrderService.getPositions(id, RemapQuerySupport.includeStock(fields));
    }

    @GetMapping("/{id}/positions/{positionId}")
    public Map<String, Object> getPosition(
            @PathVariable UUID id,
            @PathVariable UUID positionId,
            @RequestParam(required = false) String fields
    ) {
        return customerOrderService.getPosition(id, positionId, RemapQuerySupport.includeStock(fields));
    }

    @PutMapping("/{id}/positions/{positionId}")
    public Map<String, Object> updatePosition(
            @PathVariable UUID id,
            @PathVariable UUID positionId,
            @RequestBody Map<String, Object> body
    ) {
        return customerOrderService.updatePosition(id, positionId, body);
    }
}
