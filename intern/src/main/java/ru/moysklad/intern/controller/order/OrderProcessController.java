package ru.moysklad.intern.controller.order;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.moysklad.intern.dto.order.OrderProcessResponse;
import ru.moysklad.intern.service.order.OrderProcessService;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders/{orderId}/process")
public class OrderProcessController {

    private final OrderProcessService orderProcessService;

    public OrderProcessController(OrderProcessService orderProcessService) {
        this.orderProcessService = orderProcessService;
    }

    /**
     запуск обработки заказа.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderProcessResponse startProcess(@PathVariable UUID orderId) {
        return orderProcessService.startProcess(orderId);
    }

    /**
     получение текущего состояния процесса.
     */
    @GetMapping
    public OrderProcessResponse getStatus(@PathVariable UUID orderId) {
        return orderProcessService.getStatus(orderId);
    }

    /**
     подтверждение отгрузки.
     */
    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderProcessResponse confirmDispatch(@PathVariable UUID orderId) {
        return orderProcessService.confirmDispatch(orderId);
    }

    /**
     отклонение отгрузки.
     */
    @PostMapping("/reject")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderProcessResponse rejectDispatch(@PathVariable UUID orderId) {
        return orderProcessService.rejectDispatch(orderId);
    }

    /**
     запрос отмены процесса.
     */
    @PostMapping("/cancel")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderProcessResponse cancelProcess(@PathVariable UUID orderId) {
        return orderProcessService.cancelProcess(orderId);
    }

    /**
     получение итогового результата.
     */
    @GetMapping("/result")
    public OrderProcessResponse getResult(@PathVariable UUID orderId) {
        return orderProcessService.getResult(orderId);
    }
}
