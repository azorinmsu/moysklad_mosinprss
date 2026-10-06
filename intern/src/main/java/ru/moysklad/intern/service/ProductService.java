package ru.moysklad.intern.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;
import ru.moysklad.intern.config.Configuration;
import ru.moysklad.intern.dto.product.ProductRequest;
import ru.moysklad.intern.dto.product.ProductResponse;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.meta.Currency;
import ru.moysklad.intern.entity.meta.UnitOfMeasurement;
import ru.moysklad.intern.exception.IDNotProvided;
import ru.moysklad.intern.exception.UnauthorizedException;
import ru.moysklad.intern.repo.ProductRepository;
import ru.moysklad.intern.repo.meta.CurrencyRepository;
import ru.moysklad.intern.repo.meta.UnitOfMeasurementRepository;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final UnitOfMeasurementRepository unitOfMeasurementRepository;
    private final CurrencyRepository currencyRepository;

    private final Configuration configuration;

    public ProductService(ProductRepository productRepository,
                          UnitOfMeasurementRepository unitOfMeasurementRepository,
                          CurrencyRepository currencyRepository,
                          Configuration configuration) {
        this.productRepository = productRepository;
        this.unitOfMeasurementRepository = unitOfMeasurementRepository;
        this.currencyRepository = currencyRepository;
        this.configuration = configuration;
    }

    public boolean isAuthorized(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) { return false; }
        String expected = configuration.credentialsToBase64();
        return expected.equals(authHeader);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(String authHeader, String id) {
        if (!isAuthorized(authHeader))
            throw new UnauthorizedException("Неверные данные для авторизации");
        if (id.isBlank()) throw new IDNotProvided("Требуется ID");
        Product p = productRepository.findById(UUID.fromString(id)).orElseThrow();
        return ProductResponse.from(p);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(String authHeader, Pageable pageable) {
        if (!isAuthorized(authHeader))
            throw new UnauthorizedException("Неверные данные для авторизации");
        Page<Product> pList = productRepository.findAll(pageable);
        return pList.map(ProductResponse::from);
    }

    @Transactional
    public void removeProduct(String authHeader, String id) {
        if (!isAuthorized(authHeader))
            throw new UnauthorizedException("Неверные данные для авторизации");
        if (id.isBlank()) throw new IDNotProvided("Требуется ID");
        Product res = productRepository.findById(UUID.fromString(id)).orElseThrow();
        res.setArchived(true);
        productRepository.save(res);
    }

    // for put method
    @Transactional
    public ProductResponse updateFullyProduct(String authHeader, String id, ProductRequest updates) {
        if (!isAuthorized(authHeader))
            throw new UnauthorizedException("Неверные данные для авторизации");
        if (id.isBlank()) throw new IDNotProvided("Требуется ID");

        UnitOfMeasurement uom =
                unitOfMeasurementRepository.findByName(updates.uom());
        Currency currency =
                currencyRepository.findByName(updates.currency());

        Product p = productRepository.findById(UUID.fromString(id)).orElseThrow();

        p.setName(updates.name());
        if (!updates.description().isBlank()) p.setDescription(updates.description());
        p.setUom(uom); // Здесь должна быть логика добавления по имени uuid...
        p.setCurrency(currency);
        p.setPrice(new BigDecimal(updates.price()));

        productRepository.save(p);
        return ProductResponse.from(p);
    }

    // for patch method
    @Transactional
    public ProductResponse updatePartiallyProduct(String authHeader, String id, Map<String, Object> updates) {
        if (!isAuthorized(authHeader))
            throw new UnauthorizedException("Неверные данные для авторизации");
        if (id.isBlank()) throw new IDNotProvided("Требуется ID");

        Product p = productRepository.findById(UUID.fromString(id)).orElseThrow();

        updates.forEach((key, val) -> {
            Field field = ReflectionUtils.findField(Product.class, key);
            if (field != null) {
                field.setAccessible(true);
                ReflectionUtils.setField(field, p, val);
            }
        });
        productRepository.save(p);
        return ProductResponse.from(p);
    }

    @Transactional
    public ProductResponse createProduct(String authHeader, ProductRequest body) {
        if (!isAuthorized(authHeader))
            throw new UnauthorizedException("Неверные данные для авторизации");

        UnitOfMeasurement uom =
                unitOfMeasurementRepository.findByName(body.uom());
        Currency currency =
                currencyRepository.findByName(body.currency());

        Product p = new Product();
        p.setName(body.name());
        if (!body.description().isBlank()) p.setDescription(body.description());
        p.setUom(uom); // Здесь должна быть логика добавления по имени uuid...
        p.setCurrency(currency);
        p.setPrice(new BigDecimal(body.price()));

        productRepository.save(p);
        return ProductResponse.from(p);
    }
}
