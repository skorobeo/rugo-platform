package ru.rugo.core.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * HMAC-хеширование идентификаторов для аналитики. Единый источник и для записи событий
 * ({@code rugo-app}), и для фильтрации статистики ({@code rugo-admin}).
 *
 * <p>Формат входа менять нельзя — иначе фильтр перестанет совпадать с уже записанными хешами:
 * <ul>
 *   <li>{@code "u:<maxUserId>"} — авторизованный пользователь;</li>
 *   <li>{@code "s:<sessionId>"} — анонимная сессия;</li>
 *   <li>{@code "anon"} — без идентификаторов.</li>
 * </ul>
 */
public final class AnalyticsHash {

    private static final String HMAC = "HmacSHA256";

    private AnalyticsHash() {
    }

    public static String userHash(String salt, Long maxUserId) {
        return hmac(salt, "u:" + maxUserId);
    }

    public static String sessionHash(String salt, String sessionId) {
        return hmac(salt, "s:" + sessionId);
    }

    public static String anonHash(String salt) {
        return hmac(salt, "anon");
    }

    private static String hmac(String salt, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            // Пустая соль недопустима для SecretKeySpec — используем безопасный запас.
            byte[] key = (salt == null || salt.isEmpty())
                    ? new byte[]{0}
                    : salt.getBytes(StandardCharsets.UTF_8);
            mac.init(new SecretKeySpec(key, HMAC));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
