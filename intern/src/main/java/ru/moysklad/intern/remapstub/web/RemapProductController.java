package ru.moysklad.intern.remapstub.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.moysklad.intern.remapstub.service.RemapProductService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/remap/1.2/entity/product")
@ConditionalOnProperty(prefix = "app.remap-stub", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RemapProductController {

    private final RemapProductService productService;

    public RemapProductController(RemapProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(@PathVariable UUID id) {
        return productService.getById(id);
    }
}
