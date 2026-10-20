package ru.moysklad.intern.remapstub.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;
import ru.moysklad.intern.remapstub.entity.StubFaultRule;
import ru.moysklad.intern.remapstub.service.RemapFaultService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Управляемая эмуляция сбоев Remap API. Применяется только к {@code /api/remap/1.2/**}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@ConditionalOnProperty(prefix = "app.remap-stub", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RemapFaultFilter extends OncePerRequestFilter {

    private final RemapFaultService faultService;

    public RemapFaultFilter(RemapFaultService faultService) {
        this.faultService = faultService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/remap/1.2/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Optional<RemapFaultService.FaultMatch> match =
                faultService.consumeMatchingRule(request.getMethod(), request.getRequestURI());

        if (match.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        RemapFaultService.FaultMatch fault = match.get();
        switch (fault.type()) {
            case HTTP_STATUS -> {
                sleepQuietly(fault.delayMs());
                writeError(response, fault.httpStatus() == null ? 500 : fault.httpStatus());
            }
            case DELAY -> {
                sleepQuietly(fault.delayMs() > 0 ? fault.delayMs() : 2000L);
                filterChain.doFilter(request, response);
            }
            case TIMEOUT -> {
                // Долгая блокировка без ответа: клиент с коротким read-timeout получит timeout.
                sleepQuietly(fault.delayMs() > 0 ? fault.delayMs() : 60_000L);
                // После ожидания всё же отвечаем, чтобы поток не висел вечно в тестах без клиентского таймаута.
                writeError(response, 503);
            }
            case CONNECTION_RESET -> {
                sleepQuietly(fault.delayMs());
                response.resetBuffer();
                response.setStatus(HttpServletResponse.SC_OK);
                try {
                    response.getOutputStream().write(new byte[]{'{'});
                    response.flushBuffer();
                    response.getOutputStream().close();
                } catch (IOException ignored) {
                    // ожидаемо: имитация обрыва
                }
            }
            case LOSE_RESPONSE -> {
                // Операция выполняется и коммитится, но успешный HTTP-ответ клиенту не отдаётся.
                ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
                filterChain.doFilter(request, wrapped);
                // Не копируем тело в реальный response — клиент увидит обрыв / пустой ответ.
                response.resetBuffer();
                try {
                    response.getOutputStream().close();
                } catch (IOException ignored) {
                    // ignore
                }
            }
        }
    }

    private static void writeError(HttpServletResponse response, int status) throws IOException {
        response.resetBuffer();
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String body = "{\"errors\":[{\"error\":\"Simulated fault from remap stub\",\"code\":1000,\"moreInfo\":\"stub\"}]}";
        response.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        response.flushBuffer();
    }

    private static void sleepQuietly(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
