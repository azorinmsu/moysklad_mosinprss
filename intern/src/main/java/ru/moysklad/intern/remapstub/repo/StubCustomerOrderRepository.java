package ru.moysklad.intern.remapstub.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.moysklad.intern.remapstub.entity.StubCustomerOrder;

import java.util.UUID;

public interface StubCustomerOrderRepository extends JpaRepository<StubCustomerOrder, UUID> {
}
