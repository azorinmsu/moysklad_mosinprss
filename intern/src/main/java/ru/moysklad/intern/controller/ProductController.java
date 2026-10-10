package ru.moysklad.intern.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.moysklad.intern.dto.product.*;
import ru.moysklad.intern.service.ProductService;

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
        return productService.getProduct(authHeader, id);
    }

    // uses page, size and sort parameters
    @GetMapping
    public @ResponseBody Page<ProductResponse> getListOfProducts(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader,
            Pageable pageable) {
        return productService.getProducts(authHeader, pageable);
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public @ResponseBody void deleteProduct(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION) String authHeader,
            @PathVariable("id") String id) {
        productService.removeProduct(authHeader, id);
    }
}
