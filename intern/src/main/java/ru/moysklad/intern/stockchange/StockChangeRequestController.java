package ru.moysklad.intern.stockchange;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.moysklad.intern.dto.stockchange.StockChangeRequest;
import ru.moysklad.intern.dto.stockchange.StockChangeRequestAccepted;

@RestController
@RequestMapping("/api/stock-change-requests")
public class StockChangeRequestController {

    private final StockChangeRequestService stockChangeRequestService;

    public StockChangeRequestController(StockChangeRequestService stockChangeRequestService) {
        this.stockChangeRequestService = stockChangeRequestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public StockChangeRequestAccepted submit(@RequestBody StockChangeRequest request) {
        return new StockChangeRequestAccepted(stockChangeRequestService.submit(request));
    }
}
