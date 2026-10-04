package ru.moysklad.intern.services;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.moysklad.intern.config.Configuration;
import ru.moysklad.intern.dto.product.TestResponse;
import ru.moysklad.intern.repos.ProductRepository;
import ru.moysklad.intern.services.versioning.V1APIService;
import ru.moysklad.intern.utils.auth.Authorization;
import ru.moysklad.intern.utils.auth.Status;


@RestController
@RequestMapping("/product")
public class ProductService {
    private ProductRepository productRepository;

    @GetMapping("/test")
    public ResponseEntity<?> testFunc(
            @RequestHeader(value = "Authorization", required = true) String auth,
            @RequestBody String data
    ) {
        String token = auth.substring(6);
        if (Authorization.checkAuth(token) != Status.OK) {
            System.out.println(token);
            System.out.println(Authorization.checkAuth(token));
            System.out.println(new Configuration().credentialsToBase64());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("необходима авторизация");
        } else {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(new TestResponse("good"));
        }
    }
}
