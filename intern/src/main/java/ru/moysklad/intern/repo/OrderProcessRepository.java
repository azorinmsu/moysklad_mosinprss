package ru.moysklad.intern.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.moysklad.intern.entity.order.OrderProcessState;
import ru.moysklad.intern.entity.order.OrderProcessStatus;

import java.util.Optional;
import java.util.UUID;

public interface OrderProcessRepository extends JpaRepository<OrderProcessState, UUID> {
    Optional<OrderProcessState> findByProcessInstanceId(String processInstanceId);

    @Query("select count(s) > 0 from OrderProcessState s where s.orderId = :orderId and s.status = :status")
    boolean existsByOrderIdAndStatus(@Param("orderId") UUID orderId, @Param("status") OrderProcessStatus status);

    default boolean hasActiveProcess(UUID orderId) {
        return existsByOrderIdAndStatus(orderId, OrderProcessStatus.PROCESSING);
    }
}
