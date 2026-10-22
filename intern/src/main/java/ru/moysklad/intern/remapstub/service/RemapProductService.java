package ru.moysklad.intern.remapstub.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.remapstub.entity.StubProduct;
import ru.moysklad.intern.remapstub.repo.StubProductRepository;

import java.util.Map;
import java.util.UUID;

@Service
public class RemapProductService {

    private final StubProductRepository productRepository;
    private final RemapJsonMapper jsonMapper;

    public RemapProductService(StubProductRepository productRepository, RemapJsonMapper jsonMapper) {
        this.productRepository = productRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getById(UUID id) {
        StubProduct product = productRepository.findById(id)
                .orElseThrow(() -> RemapStubException.notFound("Товар не найден: " + id));
        return jsonMapper.product(product);
    }
}
