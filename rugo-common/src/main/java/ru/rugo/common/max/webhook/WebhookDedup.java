package ru.rugo.common.max.webhook;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Дедупликация входящих webhook-запросов MAX.
 *
 * <p>MAX повторяет доставку до 10 раз, поэтому одно и то же тело может прийти
 * несколько раз. Ключ — SHA-256 сырого тела: повторная доставка байт-в-байт
 * совпадает. Запись добавляется только после успешной обработки, чтобы при
 * ошибке (HTTP 500) MAX мог повторить запрос.</p>
 */
@Component
public class WebhookDedup {

    private static final long TTL_MILLIS = Duration.ofMinutes(30).toMillis();
    private static final int MAX_ENTRIES = 10_000;

    private final Map<String, Long> seen = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    public synchronized boolean isDuplicate(String key) {
        Long processedAt = seen.get(key);
        if (processedAt == null) {
            return false;
        }
        if (System.currentTimeMillis() - processedAt > TTL_MILLIS) {
            seen.remove(key);
            return false;
        }
        return true;
    }

    public synchronized void markProcessed(String key) {
        seen.put(key, System.currentTimeMillis());
    }

    public static String hash(String body) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
