package ru.moysklad.intern.service.meta;

import org.springframework.web.reactive.function.client.WebClient;
import ru.moysklad.intern.config.WebClientConfiguration;
import ru.moysklad.intern.dto.uom.UomDto;
import ru.moysklad.intern.dto.uom.UomListDto;
import ru.moysklad.intern.entity.meta.UnitOfMeasurement;
import ru.moysklad.intern.repo.meta.UnitOfMeasurementRepository;

import java.util.Map;
import java.util.UUID;

public class UnitOfMeasurementService {
    private final WebClient webClient;
    private final UnitOfMeasurementRepository uomRepository;

    public UnitOfMeasurementService(WebClient webClient,
                                    UnitOfMeasurementRepository uomRepository) {
        this.webClient = webClient;
        this.uomRepository = uomRepository;
    }

    public void syncUOM() {
        webClient.get()
                .uri("/entity/uom")
                .retrieve()
                .bodyToMono(UomListDto.class)
                .doOnNext(response -> {
                    response.rows().forEach(dto -> {
                        UnitOfMeasurement entity = uomRepository.findByExternalId(UUID.fromString(dto.externalId()));
                        if (entity == null) new UnitOfMeasurement();
                        entity.setName(dto.name());
                        entity.setDescription(dto.description());
                        entity.setExternalId(UUID.fromString(dto.externalId()));
                        uomRepository.save(entity);
                    });
                })
                .block();
    }
}
