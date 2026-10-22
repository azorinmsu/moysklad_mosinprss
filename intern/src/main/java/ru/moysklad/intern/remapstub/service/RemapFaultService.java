package ru.moysklad.intern.remapstub.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.remapstub.entity.StubFaultRule;
import ru.moysklad.intern.remapstub.repo.StubFaultRuleRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class RemapFaultService {

    public record FaultMatch(
            StubFaultRule.FaultType type,
            Integer httpStatus,
            long delayMs
    ) {
    }

    private final StubFaultRuleRepository faultRuleRepository;

    public RemapFaultService(StubFaultRuleRepository faultRuleRepository) {
        this.faultRuleRepository = faultRuleRepository;
    }

    @Transactional
    public Map<String, Object> addRule(Map<String, Object> body) {
        StubFaultRule rule = new StubFaultRule();
        rule.setId(UUID.randomUUID());
        rule.setMethod(String.valueOf(body.getOrDefault("method", "*")).toUpperCase(Locale.ROOT));
        rule.setPathPattern(String.valueOf(body.get("pathPattern")));
        rule.setFaultType(StubFaultRule.FaultType.valueOf(String.valueOf(body.get("faultType")).toUpperCase(Locale.ROOT)));
        rule.setRemainingCount(body.containsKey("count") ? ((Number) body.get("count")).intValue() : 1);
        rule.setDelayMs(body.containsKey("delayMs") ? ((Number) body.get("delayMs")).longValue() : 0L);
        if (body.get("httpStatus") != null) {
            rule.setHttpStatus(((Number) body.get("httpStatus")).intValue());
        } else if (rule.getFaultType() == StubFaultRule.FaultType.HTTP_STATUS) {
            throw RemapStubException.badRequest("httpStatus обязателен для faultType=HTTP_STATUS");
        }
        rule.setCreatedAt(LocalDateTime.now());
        faultRuleRepository.save(rule);
        return toDto(rule);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRules() {
        return faultRuleRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public void clearRules() {
        faultRuleRepository.deleteAllInBatch();
    }

    /**
     * Атомарно выбирает и «списывает» одно срабатывание правила для запроса.
     */
    @Transactional
    public Optional<FaultMatch> consumeMatchingRule(String method, String requestPath) {
        List<StubFaultRule> rules = faultRuleRepository.findByRemainingCountGreaterThanOrderByCreatedAtAsc(0);
        for (StubFaultRule rule : rules) {
            if (!methodMatches(rule.getMethod(), method)) {
                continue;
            }
            if (!pathMatches(rule.getPathPattern(), requestPath)) {
                continue;
            }
            rule.setRemainingCount(rule.getRemainingCount() - 1);
            faultRuleRepository.save(rule);
            return Optional.of(new FaultMatch(rule.getFaultType(), rule.getHttpStatus(), rule.getDelayMs()));
        }
        return Optional.empty();
    }

    private boolean methodMatches(String pattern, String method) {
        return "*".equals(pattern) || pattern.equalsIgnoreCase(method);
    }

    private boolean pathMatches(String pattern, String path) {
        if (pattern == null || pattern.isBlank()) {
            return false;
        }
        if (pattern.equals(path)) {
            return true;
        }
        // Ant-like: * = любой сегмент без '/', ** = что угодно
        String regex = pattern
                .replace(".", "\\.")
                .replace("**", "§§")
                .replace("*", "[^/]*")
                .replace("§§", ".*");
        return Pattern.compile("^" + regex + "$").matcher(path).matches();
    }

    private Map<String, Object> toDto(StubFaultRule rule) {
        return Map.of(
                "id", rule.getId().toString(),
                "method", rule.getMethod(),
                "pathPattern", rule.getPathPattern(),
                "faultType", rule.getFaultType().name(),
                "remainingCount", rule.getRemainingCount(),
                "delayMs", rule.getDelayMs(),
                "httpStatus", rule.getHttpStatus() == null ? 0 : rule.getHttpStatus()
        );
    }
}
