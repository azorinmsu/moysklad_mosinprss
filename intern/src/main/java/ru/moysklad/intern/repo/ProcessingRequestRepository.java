package ru.moysklad.intern.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.moysklad.intern.entity.processing.ProcessingCommandType;
import ru.moysklad.intern.entity.processing.ProcessingRequest;
import ru.moysklad.intern.entity.processing.ProcessingRequestStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessingRequestRepository extends JpaRepository<ProcessingRequest, UUID> {

    Optional<ProcessingRequest> findByRequestId(UUID requestId);

    boolean existsByRequestId(UUID requestId);

    List<ProcessingRequest> findByCommandTypeAndStatusOrderByCreatedAtAsc(
            ProcessingCommandType commandType,
            ProcessingRequestStatus status
    );

    @Query(value = """
            select * from processing_request
            where command_type = 'SALES' and status = 'PENDING'
            order by created_at asc
            limit 1
            for update skip locked
            """, nativeQuery = true) // попробовать оптимистичную блокировку?
    Optional<ProcessingRequest> claimNextPendingSale();
}
