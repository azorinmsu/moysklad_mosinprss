package ru.moysklad.intern.remapstub.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.moysklad.intern.remapstub.entity.StubFaultRule;

import java.util.List;
import java.util.UUID;

public interface StubFaultRuleRepository extends JpaRepository<StubFaultRule, UUID> {

    List<StubFaultRule> findByRemainingCountGreaterThanOrderByCreatedAtAsc(int remainingCount);
}
