package ru.moysklad.intern.remapstub.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.remapstub.entity.StubCustomerOrder;
import ru.moysklad.intern.remapstub.entity.StubOrderPosition;
import ru.moysklad.intern.remapstub.entity.StubStock;
import ru.moysklad.intern.remapstub.repo.StubCustomerOrderRepository;
import ru.moysklad.intern.remapstub.repo.StubOrderPositionRepository;
import ru.moysklad.intern.remapstub.repo.StubStockRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RemapCustomerOrderService {

    private final StubCustomerOrderRepository orderRepository;
    private final StubOrderPositionRepository positionRepository;
    private final StubStockRepository stockRepository;
    private final RemapStockService stockService;
    private final RemapJsonMapper jsonMapper;
    private final RemapMetaFactory metaFactory;

    public RemapCustomerOrderService(
            StubCustomerOrderRepository orderRepository,
            StubOrderPositionRepository positionRepository,
            StubStockRepository stockRepository,
            RemapStockService stockService,
            RemapJsonMapper jsonMapper,
            RemapMetaFactory metaFactory
    ) {
        this.orderRepository = orderRepository;
        this.positionRepository = positionRepository;
        this.stockRepository = stockRepository;
        this.stockService = stockService;
        this.jsonMapper = jsonMapper;
        this.metaFactory = metaFactory;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getById(UUID id, boolean expandPositions, boolean includeStock) {
        StubCustomerOrder order = requireOrder(id);
        List<StubOrderPosition> positions = positionRepository.findByOrderIdOrderByIdAsc(id);
        return jsonMapper.customerOrder(order, positions, loadStocks(positions), expandPositions, includeStock);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPositions(UUID orderId, boolean includeStock) {
        requireOrder(orderId);
        List<StubOrderPosition> positions = positionRepository.findByOrderIdOrderByIdAsc(orderId);
        Map<UUID, StubStock> stocks = loadStocks(positions);
        List<Map<String, Object>> rows = positions.stream()
                .map(p -> jsonMapper.orderPosition(p, stocks.get(p.getProductId()), includeStock))
                .toList();
        return metaFactory.listEntity(
                "customerorderposition",
                "/entity/customerorder/" + orderId + "/positions",
                rows
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPosition(UUID orderId, UUID positionId, boolean includeStock) {
        requireOrder(orderId);
        StubOrderPosition position = positionRepository.findById(positionId)
                .filter(p -> p.getOrderId().equals(orderId))
                .orElseThrow(() -> RemapStubException.notFound("Позиция заказа не найдена: " + positionId));
        StubStock stock = stockRepository.findById(position.getProductId()).orElse(null);
        return jsonMapper.orderPosition(position, stock, includeStock);
    }

    /**
     * Резервирование / изменение резерва позиции заказа — через поле {@code reserve}
     * (как в реальном Remap API, без вымышленных /reserve endpoints).
     */
    @Transactional
    public Map<String, Object> updatePosition(UUID orderId, UUID positionId, Map<String, Object> body) {
        requireOrder(orderId);
        StubOrderPosition position = positionRepository.findByIdForUpdate(positionId)
                .filter(p -> p.getOrderId().equals(orderId))
                .orElseThrow(() -> RemapStubException.notFound("Позиция заказа не найдена: " + positionId));

        if (body.containsKey("reserve")) {
            BigDecimal newReserve = toDecimal(body.get("reserve"));
            if (newReserve.compareTo(BigDecimal.ZERO) < 0) {
                throw RemapStubException.badRequest("reserve не может быть отрицательным");
            }
            if (newReserve.compareTo(position.getQuantity()) > 0) {
                throw RemapStubException.badRequest("reserve не может превышать quantity позиции");
            }
            BigDecimal delta = newReserve.subtract(position.getReserve());
            if (delta.compareTo(BigDecimal.ZERO) != 0) {
                stockService.applyReserveDelta(position.getProductId(), delta);
                position.setReserve(newReserve);
            }
        }

        if (body.containsKey("quantity")) {
            position.setQuantity(toDecimal(body.get("quantity")));
        }
        if (body.containsKey("price")) {
            position.setPrice(toLong(body.get("price")));
        }

        positionRepository.save(position);
        StubStock stock = stockRepository.findById(position.getProductId()).orElse(null);
        return jsonMapper.orderPosition(position, stock, false);
    }

    /**
     * Частичное обновление заказа. Поддерживает изменение резервов через positions.rows[].reserve.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> updateOrder(UUID orderId, Map<String, Object> body) {
        StubCustomerOrder order = requireOrder(orderId);
        if (body.containsKey("name")) {
            order.setName(String.valueOf(body.get("name")));
        }
        if (body.containsKey("description")) {
            order.setDescription(body.get("description") == null ? null : String.valueOf(body.get("description")));
        }
        if (body.containsKey("applicable")) {
            order.setApplicable(Boolean.parseBoolean(String.valueOf(body.get("applicable"))));
        }
        orderRepository.save(order);

        Object positionsNode = body.get("positions");
        if (positionsNode instanceof Map<?, ?> positionsMap) {
            Object rows = positionsMap.get("rows");
            if (rows instanceof List<?> list) {
                for (Object item : list) {
                    if (!(item instanceof Map<?, ?> raw)) {
                        continue;
                    }
                    Map<String, Object> row = (Map<String, Object>) raw;
                    Object idValue = row.get("id");
                    if (idValue == null) {
                        continue;
                    }
                    updatePosition(orderId, UUID.fromString(String.valueOf(idValue)), row);
                }
            }
        }

        List<StubOrderPosition> positions = positionRepository.findByOrderIdOrderByIdAsc(orderId);
        return jsonMapper.customerOrder(order, positions, loadStocks(positions), true, false);
    }

    private StubCustomerOrder requireOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> RemapStubException.notFound("Заказ покупателя не найден: " + id));
    }

    private Map<UUID, StubStock> loadStocks(List<StubOrderPosition> positions) {
        Map<UUID, StubStock> stocks = new HashMap<>();
        for (StubOrderPosition position : positions) {
            stockRepository.findById(position.getProductId())
                    .ifPresent(stock -> stocks.put(position.getProductId(), stock));
        }
        return stocks;
    }

    private static BigDecimal toDecimal(Object value) {
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
