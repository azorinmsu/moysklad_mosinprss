package ru.moysklad.intern.remapstub.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.moysklad.intern.remapstub.service.RemapFaultService;
import ru.moysklad.intern.remapstub.service.RemapStubSeedService;

import java.util.List;
import java.util.Map;

/**
 * Технический API управления заглушкой. Не является частью Remap API 1.2.
 */
@RestController
@RequestMapping("/api/stub")
@ConditionalOnProperty(prefix = "app.remap-stub", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RemapStubAdminController {

    private final RemapStubSeedService seedService;
    private final RemapFaultService faultService;

    public RemapStubAdminController(RemapStubSeedService seedService, RemapFaultService faultService) {
        this.seedService = seedService;
        this.faultService = faultService;
    }

    @PostMapping("/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset() {
        seedService.resetToSeed();
        faultService.clearRules();
    }

    @GetMapping("/faults")
    public List<Map<String, Object>> listFaults() {
        return faultService.listRules();
    }

    @PostMapping("/faults")
    public Map<String, Object> addFault(@RequestBody Map<String, Object> body) {
        return faultService.addRule(body);
    }

    @DeleteMapping("/faults")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearFaults() {
        faultService.clearRules();
    }
}
