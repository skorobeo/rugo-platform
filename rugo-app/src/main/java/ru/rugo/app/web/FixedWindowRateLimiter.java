package ru.rugo.app.web;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Простейший ограничитель частоты (fixed window) в памяти.
 * Подходит для одного инстанса (MVP). Ключи — «ip:…» / «user:…».
 */
@Component
public class FixedWindowRateLimiter {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public boolean tryAcquire(String key, int max, int windowSeconds) {
        long now = System.currentTimeMillis();
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(w -> now - w.start >= windowSeconds * 1000L);
        }
        Window window = windows.compute(key, (k, current) -> {
            if (current == null || now - current.start >= windowSeconds * 1000L) {
                return new Window(now);
            }
            current.count.incrementAndGet();
            return current;
        });
        return window.count.get() <= max;
    }

    private static final class Window {
        private final long start;
        private final AtomicInteger count = new AtomicInteger(1);

        private Window(long start) {
            this.start = start;
        }
    }
}
