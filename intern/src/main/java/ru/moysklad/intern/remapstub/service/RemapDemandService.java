package ru.moysklad.intern.remapstub.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.remapstub.entity.StubCustomerOrder;
import ru.moysklad.intern.remapstub.entity.StubDemand;
import ru.moysklad.intern.remapstub.entity.StubDemandPosition;
import ru.moysklad.intern.remapstub.entity.StubOrderPosition;
import ru.moysklad.intern.remapstub.repo.StubCustomerOrderRepository;
import ru.moysklad.intern.remapstub.repo.StubDemandPositionRepository;
import ru.moysklad.intern.remapstub.repo.StubDemandRepository;
import ru.moysklad.intern.remapstub.repo.StubOrderPositionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RemapDemandService {

    private final StubDemandRepository demandRepository;
    private final StubDemandPositionRepository demandPositionRepository;
    private final StubCustomerOrderRepository orderRepository;
    private final StubOrderPositionRepository orderPositionRepository;
    private final RemapStockService stockService;
    private final RemapJsonMapper jsonMapper;
    private final RemapMetaFactory metaFactory;

    public RemapDemandService(
            StubDemandRepository demandRepository,
            StubDemandPositionRepository demandPositionRepository,
            StubCustomerOrderRepository orderRepository,
            StubOrderPositionRepository orderPositionRepository,
            RemapStockService stockService,
            RemapJsonMapper jsonMapper,
            RemapMetaFactory metaFactory
    ) {
        this.demandRepository = demandRepository;
        this.demandPositionRepository = demandPositionRepository;
        this.orderRepository = orderRepository;
        this.orderPositionRepository = orderPositionRepository;
        this.stockService = stockService;
        this.jsonMapper = jsonMapper;
        this.metaFactory = metaFactory;
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> create(Map<String, Object> body) {
        StubDemand demand = new StubDemand();
        demand.setId(UUID.randomUUID());
        demand.setName(body.containsKey("name") ? String.valueOf(body.get("name")) : ("00000" + System.currentTimeMillis() % 100000));
        demand.setDescription(body.get("description") == null ? null : String.valueOf(body.get("description")));
        demand.setApplicable(body.containsKey("applicable") && Boolean.parseBoolean(String.valueOf(body.get("applicable"))));
        demand.setMoment(LocalDateTime.now());
        demand.setOrganizationId(extractRefId(body.get("organization"), "organization"));
        demand.setAgentId(extractRefId(body.get("agent"), "counterparty"));
        demand.setStoreId(extractRefId(body.get("store"), "store"));

        UUID orderId = extractOptionalRefId(body.get("customerOrder"));
        if (orderId != null) {
            StubCustomerOrder order = orderRepository.findById(orderId)
                    .orElseThrow(() -> RemapStubException.notFound("Заказ покупателя не найден: " + orderId));
            demand.setCustomerOrderId(order.getId());
            if (demand.getOrganizationId() == null) {
                demand.setOrganizationId(order.getOrganizationId());
            }
            if (demand.getAgentId() == null) {
                demand.setAgentId(order.getAgentId());
            }
            if (demand.getStoreId() == null) {
                demand.setStoreId(order.getStoreId());
            }
        }

        if (demand.getOrganizationId() == null || demand.getAgentId() == null || demand.getStoreId() == null) {
            throw RemapStubException.badRequest("Для отгрузки обязательны organization, agent и store");
        }

        demandRepository.save(demand);

        List<StubDemandPosition> positions = buildPositions(demand, body, orderId);
        if (positions.isEmpty()) {
            throw RemapStubException.badRequest("Отгрузка должна содержать хотя бы одну позицию");
        }
        demandPositionRepository.saveAll(positions);

        if (demand.isApplicable()) {
            applyDemand(demand, positions);
        }

        return jsonMapper.demand(demand, positions, true);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getById(UUID id, boolean expandPositions) {
        StubDemand demand = requireDemand(id);
        List<StubDemandPosition> positions = demandPositionRepository.findByDemandIdOrderByIdAsc(id);
        return jsonMapper.demand(demand, positions, expandPositions);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPositions(UUID demandId) {
        requireDemand(demandId);
        List<StubDemandPosition> positions = demandPositionRepository.findByDemandIdOrderByIdAsc(demandId);
        List<Map<String, Object>> rows = positions.stream().map(jsonMapper::demandPosition).toList();
        return metaFactory.listEntity("demandposition", "/entity/demand/" + demandId + "/positions", rows);
    }

    @Transactional
    public Map<String, Object> update(UUID id, Map<String, Object> body) {
        StubDemand demand = demandRepository.findByIdForUpdate(id)
                .orElseThrow(() -> RemapStubException.notFound("Отгрузка не найдена: " + id));
        List<StubDemandPosition> positions = demandPositionRepository.findByDemandIdOrderByIdAsc(id);

        boolean wasApplicable = demand.isApplicable();
        if (body.containsKey("name")) {
            demand.setName(String.valueOf(body.get("name")));
        }
        if (body.containsKey("description")) {
            demand.setDescription(body.get("description") == null ? null : String.valueOf(body.get("description")));
        }
        if (body.containsKey("applicable")) {
            demand.setApplicable(Boolean.parseBoolean(String.valueOf(body.get("applicable"))));
        }

        if (!wasApplicable && demand.isApplicable()) {
            applyDemand(demand, positions);
        } else if (wasApplicable && !demand.isApplicable()) {
            unapplyDemand(demand, positions);
        }

        demandRepository.save(demand);
        return jsonMapper.demand(demand, positions, true);
    }

    /**
     * Удаление непроведённой отгрузки (черновик). Проведённую удалять нельзя без отмены проведения.
     */
    @Transactional
    public void delete(UUID id) {
        StubDemand demand = demandRepository.findByIdForUpdate(id)
                .orElseThrow(() -> RemapStubException.notFound("Отгрузка не найдена: " + id));
        if (demand.isApplicable()) {
            throw RemapStubException.conflict("Нельзя удалить проведённую отгрузку. Сначала снимите applicable=false");
        }
        demandPositionRepository.deleteByDemandId(id);
        demandRepository.delete(demand);
    }

    private void applyDemand(StubDemand demand, List<StubDemandPosition> positions) {
        for (StubDemandPosition position : positions) {
            BigDecimal reserveToRelease = BigDecimal.ZERO;
            if (demand.getCustomerOrderId() != null) {
                reserveToRelease = releaseOrderReserve(demand.getCustomerOrderId(), position.getProductId(), position.getQuantity());
            }
            stockService.applyShip(position.getProductId(), position.getQuantity(), reserveToRelease);
        }
    }

    private void unapplyDemand(StubDemand demand, List<StubDemandPosition> positions) {
        for (StubDemandPosition position : positions) {
            BigDecimal reserveToRestore = BigDecimal.ZERO;
            if (demand.getCustomerOrderId() != null) {
                reserveToRestore = restoreOrderReserve(demand.getCustomerOrderId(), position.getProductId(), position.getQuantity());
            }
            stockService.restoreShip(position.getProductId(), position.getQuantity(), reserveToRestore);
        }
    }

    private BigDecimal releaseOrderReserve(UUID orderId, UUID productId, BigDecimal quantity) {
        List<StubOrderPosition> positions = orderPositionRepository.findByOrderIdOrderByIdAsc(orderId);
        BigDecimal remaining = quantity;
        BigDecimal released = BigDecimal.ZERO;
        for (StubOrderPosition position : positions) {
            if (!position.getProductId().equals(productId) || remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            StubOrderPosition locked = orderPositionRepository.findByIdForUpdate(position.getId()).orElse(position);
            BigDecimal release = locked.getReserve().min(remaining);
            locked.setReserve(locked.getReserve().subtract(release));
            locked.setShipped(locked.getShipped().add(release.min(quantity)));
            orderPositionRepository.save(locked);
            released = released.add(release);
            remaining = remaining.subtract(release);
        }
        return released;
    }

    private BigDecimal restoreOrderReserve(UUID orderId, UUID productId, BigDecimal quantity) {
        List<StubOrderPosition> positions = orderPositionRepository.findByOrderIdOrderByIdAsc(orderId);
        BigDecimal remaining = quantity;
        BigDecimal restored = BigDecimal.ZERO;
        for (StubOrderPosition position : positions) {
            if (!position.getProductId().equals(productId) || remaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            StubOrderPosition locked = orderPositionRepository.findByIdForUpdate(position.getId()).orElse(position);
            BigDecimal restore = remaining.min(locked.getQuantity().subtract(locked.getReserve()));
            locked.setReserve(locked.getReserve().add(restore));
            locked.setShipped(locked.getShipped().subtract(restore).max(BigDecimal.ZERO));
            orderPositionRepository.save(locked);
            restored = restored.add(restore);
            remaining = remaining.subtract(restore);
        }
        return restored;
    }

    @SuppressWarnings("unchecked")
    private List<StubDemandPosition> buildPositions(StubDemand demand, Map<String, Object> body, UUID orderId) {
        List<StubDemandPosition> result = new ArrayList<>();
        Object positionsNode = body.get("positions");
        if (positionsNode instanceof Map<?, ?> positionsMap && positionsMap.get("rows") instanceof List<?> rows) {
            for (Object item : rows) {
                if (!(item instanceof Map<?, ?> raw)) {
                    continue;
                }
                Map<String, Object> row = (Map<String, Object>) raw;
                StubDemandPosition position = new StubDemandPosition();
                position.setId(UUID.randomUUID());
                position.setDemandId(demand.getId());
                position.setProductId(extractRefId(row.get("assortment"), "product"));
                position.setQuantity(toDecimal(row.get("quantity")));
                position.setPrice(row.containsKey("price") ? toLong(row.get("price")) : 0L);
                result.add(position);
            }
            return result;
        }

        if (orderId != null) {
            for (StubOrderPosition orderPosition : orderPositionRepository.findByOrderIdOrderByIdAsc(orderId)) {
                StubDemandPosition position = new StubDemandPosition();
                position.setId(UUID.randomUUID());
                position.setDemandId(demand.getId());
                position.setProductId(orderPosition.getProductId());
                position.setQuantity(orderPosition.getQuantity());
                position.setPrice(orderPosition.getPrice());
                result.add(position);
            }
        }
        return result;
    }

    private StubDemand requireDemand(UUID id) {
        return demandRepository.findById(id)
                .orElseThrow(() -> RemapStubException.notFound("Отгрузка не найдена: " + id));
    }

    @SuppressWarnings("unchecked")
    private UUID extractRefId(Object node, String expectedType) {
        UUID id = extractOptionalRefId(node);
        if (id == null) {
            throw RemapStubException.badRequest("Не указана ссылка на " + expectedType);
        }
        return id;
    }

    @SuppressWarnings("unchecked")
    private UUID extractOptionalRefId(Object node) {
        if (node == null) {
            return null;
        }
        if (node instanceof String str) {
            return UUID.fromString(str);
        }
        if (node instanceof Map<?, ?> map) {
            if (map.get("id") != null) {
                return UUID.fromString(String.valueOf(map.get("id")));
            }
            Object meta = map.get("meta");
            if (meta instanceof Map<?, ?> metaMap && metaMap.get("href") != null) {
                String href = String.valueOf(metaMap.get("href"));
                String[] parts = href.split("/");
                return UUID.fromString(parts[parts.length - 1]);
            }
        }
        return null;
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) {
            throw RemapStubException.badRequest("quantity обязателен");
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
