package ru.moysklad.intern.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.moysklad.intern.entity.processing.ProcessingResult;

import java.util.Optional;
import java.util.UUID;

public interface ProcessingResultRepository extends JpaRepository<ProcessingResult, UUID> {

    Optional<ProcessingResult> findByRequestId(UUID requestId);

    boolean existsByRequestId(UUID requestId);
}
