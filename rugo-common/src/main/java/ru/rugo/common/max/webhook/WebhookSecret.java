package ru.rugo.common.max.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Проверка заголовка {@code X-Max-Bot-Api-Secret} без утечки длины по времени. */
public final class WebhookSecret {

    private WebhookSecret() {
    }

    public static boolean matches(String expected, String actual) {
        if (expected == null || expected.isBlank() || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
