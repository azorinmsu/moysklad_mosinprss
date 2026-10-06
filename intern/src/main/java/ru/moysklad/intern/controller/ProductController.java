package ru.moysklad.intern.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.moysklad.intern.dto.product.*;
import ru.moysklad.intern.service.ProductService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) { this.productService = productService; }

    @GetMapping("/{id}")
    public @ResponseBody ProductResponse getProductByID(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader,
            @PathVariable("id") String id) {
        return (ProductResponse) productService.getProduct(authHeader, id);
    }

    @GetMapping
    public @ResponseBody List<ProductResponse> getListOfProducts(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader) {
        return null;
    }

    @PostMapping
    public @ResponseBody ProductResponse createNewProduct(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader,
            @RequestBody ProductRequest body) {
        return productService.createProduct(authHeader, body);
    }

    @PutMapping("/{id}")
    public @ResponseBody ProductResponse updateFullyProduct(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader,
            @PathVariable("id") String id,
            @RequestBody ProductRequest updates) {
        return productService.updateFullyProduct(authHeader, id, updates);
    }

    @PatchMapping("/{id}")
    public @ResponseBody ProductResponse updatePartiallyProduct(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader,
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> updates) {
        return productService.updatePartiallyProduct(authHeader, id, updates);
    }

    @DeleteMapping("/{id}")
    public @ResponseBody HttpStatus deleteProduct(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader,
            @PathVariable("id") String id) {
        return productService.removeProduct(authHeader, id);
    }
}
