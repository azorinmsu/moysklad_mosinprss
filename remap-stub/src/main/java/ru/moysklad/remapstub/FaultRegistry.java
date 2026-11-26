package ru.moysklad.remapstub;

import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class FaultRegistry {

    public enum Type {
        HTTP_STATUS,
        DELAY,
        TIMEOUT,
        CONNECTION_RESET,
        LOSE_RESPONSE
    }

    public record Rule(String id, String method, String pathPattern, Type type, int httpStatus, int code, long delayMs, int remaining) {
        Rule consume() {
            return new Rule(id, method, pathPattern, type, httpStatus, code, delayMs, remaining - 1);
        }
    }

    private final List<Rule> rules = new ArrayList<>();
    private final AntPathMatcher matcher = new AntPathMatcher();
    private volatile boolean unavailable;

    public synchronized List<Rule> rules() {
        return List.copyOf(rules);
    }

    public synchronized Rule add(String method, String pathPattern, Type type, int httpStatus, int code, long delayMs, int count) {
        Rule rule = new Rule(java.util.UUID.randomUUID().toString(), method.toUpperCase(), pathPattern, type, httpStatus, code, delayMs, count);
        rules.add(rule);
        return rule;
    }

    public synchronized void clear() {
        rules.clear();
        unavailable = false;
    }

    public boolean unavailable() {
        return unavailable;
    }

    public void setUnavailable(boolean unavailable) {
        this.unavailable = unavailable;
    }

    public synchronized Optional<Rule> consume(String method, String path) {
        for (int i = 0; i < rules.size(); i++) {
            Rule rule = rules.get(i);
            if (!rule.method.equalsIgnoreCase(method) || !matcher.match(rule.pathPattern, path)) {
                continue;
            }
            if (rule.remaining <= 1) {
                rules.remove(i);
            } else {
                rules.set(i, rule.consume());
            }
            return Optional.of(rule);
        }
        return Optional.empty();
    }
}
