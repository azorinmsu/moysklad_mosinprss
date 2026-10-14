package ru.moysklad.intern.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.moysklad.intern.entity.operation.DocumentsWithPositions;

import java.util.List;
import java.util.UUID;

public interface DocumentsRepository extends JpaRepository<DocumentsWithPositions, UUID> {

    @Query("""
            select distinct o from DocumentsWithPositions o
            left join fetch o.motions m
            left join fetch m.lot
            left join fetch o.sourceStore
            left join fetch o.targetStore
            where o.applicable = true
            """)
    List<DocumentsWithPositions> findApplicable();
}
