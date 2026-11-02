package ru.moysklad.intern.remapstub.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.moysklad.intern.remapstub.service.RemapDemandService;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/remap/1.2/entity/demand")
@ConditionalOnProperty(prefix = "app.remap-stub", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RemapDemandController {

    private final RemapDemandService demandService;

    public RemapDemandController(RemapDemandService demandService) {
        this.demandService = demandService;
    }

    @PostMapping({"", "/"})
    @ResponseStatus(HttpStatus.OK)
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        return demandService.create(body);
    }

    @GetMapping({"", "/"})
    public Map<String, Object> list(
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String expand
    ) {
        return demandService.list(filter, RemapQuerySupport.expandPositions(expand));
    }

    @GetMapping("/{id}")
    public Map<String, Object> getById(
            @PathVariable UUID id,
            @RequestParam(required = false) String expand
    ) {
        return demandService.getById(id, RemapQuerySupport.expandPositions(expand));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable UUID id, @RequestBody Map<String, Object> body) {
        return demandService.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        demandService.delete(id);
    }

    @GetMapping("/{id}/positions")
    public Map<String, Object> getPositions(@PathVariable UUID id) {
        return demandService.getPositions(id);
    }
}
