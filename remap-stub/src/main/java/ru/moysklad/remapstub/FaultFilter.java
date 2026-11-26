package ru.moysklad.remapstub;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Component
@Order(2)
public class FaultFilter extends OncePerRequestFilter {

    private final FaultRegistry faults;

    public FaultFilter(FaultRegistry faults) {
        this.faults = faults;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/remap/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (faults.unavailable()) {
            response.setStatus(503);
            response.setContentType("application/json");
            response.getWriter().write("{\"errors\":[{\"error\":\"Сервис временно отключен\",\"code\":503}]}");
            return;
        }
        var matched = faults.consume(request.getMethod(), request.getRequestURI());
        if (matched.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        FaultRegistry.Rule rule = matched.get();
        switch (rule.type()) {
            case DELAY -> {
                sleep(rule.delayMs());
                filterChain.doFilter(request, response);
            }
            case TIMEOUT -> {
                sleep(rule.delayMs() > 0 ? rule.delayMs() : 30_000);
                response.setStatus(503);
            }
            case HTTP_STATUS -> {
                response.setStatus(rule.httpStatus());
                response.setContentType("application/json");
                int code = rule.code() != 0 ? rule.code() : (rule.httpStatus() == 429 ? 1049 : rule.httpStatus());
                response.getWriter().write("{\"errors\":[{\"error\":\"Введённый сбой заглушки\",\"code\":" + code + "}]}");
            }
            case CONNECTION_RESET -> {
                response.setHeader("X-Stub-Fault", "connection-reset");
                response.setStatus(500);
            }
            case LOSE_RESPONSE -> {
                ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
                filterChain.doFilter(request, wrapper);
                response.setStatus(wrapper.getStatus());
                response.setContentLength(0);
            }
        }
    }

    private static void sleep(long delayMs) {
        try {
            Thread.sleep(Math.max(delayMs, 0));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
