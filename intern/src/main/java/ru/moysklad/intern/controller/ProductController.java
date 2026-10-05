package ru.moysklad.intern.controller;

import org.springframework.web.bind.annotation.*;
import ru.moysklad.intern.dto.product.ProductAddNewRequest;
import ru.moysklad.intern.dto.product.ProductResponse;
import ru.moysklad.intern.service.ProductService;

@RestController
@RequestMapping("/product")
public class ProductController extends BaseController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public @ResponseBody ProductResponse createNewProduct(
            @RequestBody ProductAddNewRequest body) {
        productService.newProduct(body);
    }
}
