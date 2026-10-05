package ru.moysklad.intern.service;

import org.springframework.stereotype.Service;
import ru.moysklad.intern.config.Configuration;
import ru.moysklad.intern.dto.product.ProductAddNewRequest;
import ru.moysklad.intern.dto.product.ProductResponse;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.meta.Currency;
import ru.moysklad.intern.entity.meta.UnitOfMeasurement;
import ru.moysklad.intern.exception.UnauthorizedException;
import ru.moysklad.intern.repo.ProductRepository;
import ru.moysklad.intern.repo.meta.CurrencyRepository;
import ru.moysklad.intern.repo.meta.UnitOfMeasurementRepository;

import java.util.Optional;

@Service
public class ProductService {
    private ProductRepository productRepository;
    private UnitOfMeasurementRepository unitOfMeasurementRepository;
    private CurrencyRepository currencyRepository;

    private final Configuration configuration;

    public ProductService(Configuration configuration) {
        this.configuration = configuration;
    }

    public boolean isAuthorized(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) { return false; }
        String expected = configuration.credentialsToBase64();
        return expected.equals(authHeader);
    }

    public ProductResponse newProduct(String authHeader, ProductAddNewRequest body) {
        if (!isAuthorized(authHeader))
            throw new UnauthorizedException("Неверные данные для авторизации");

        UnitOfMeasurement uom =
                unitOfMeasurementRepository.findByName(body.uom());
        Currency currency =
                currencyRepository.findByName(body.currency());

        Product p = new Product();
        p.setName(body.name());
        if (!body.description().isBlank()) p.setDescription(body.description());
        p.setUom(); // Здесь должна быть логика добавления по имени uuid...
    }
}
