package ru.moysklad.remapstub;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
public class ProtocolFilter extends OncePerRequestFilter {

    private final StubProperties properties;

    public ProtocolFilter(StubProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/remap/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String encoding = request.getHeader("Accept-Encoding");
        if (encoding == null || !encoding.toLowerCase().contains("gzip")) {
            response.setStatus(415);
            return;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            response.setStatus(401);
            return;
        }
        String required = properties.requiredToken();
        if (!required.isBlank()) {
            if (!authorization.equals("Bearer " + required)) {
                response.setStatus(401);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
