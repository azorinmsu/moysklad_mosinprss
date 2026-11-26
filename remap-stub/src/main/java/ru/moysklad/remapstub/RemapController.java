package ru.moysklad.remapstub;

import tools.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RemapController {

    private static final byte[] PNG = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    private final RemapEngine engine;

    public RemapController(RemapEngine engine) {
        this.engine = engine;
    }

    @RequestMapping("/api/remap/1.2/**")
    public ResponseEntity<?> handle(HttpServletRequest request, @RequestBody(required = false) JsonNode body) {
        if (request.getRequestURI().contains("/download/")) {
            return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(PNG);
        }
        return engine.dispatch(request, body);
    }
}
