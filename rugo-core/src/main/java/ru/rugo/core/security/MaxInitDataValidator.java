package ru.rugo.core.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Проверка подписи initData мини-приложения MAX.
 *
 * <p>Алгоритм (см. docs/miniapp/MAX-DOCS-CHECK.md §1.4):
 * <pre>
 *   secret_key = HMAC_SHA256(key = "WebAppData", message = BOT_TOKEN)
 *   signature  = hex(HMAC_SHA256(key = secret_key, message = launch_params))
 * </pre>
 * где {@code launch_params} — пары {@code key=value}, отсортированные по ключу,
 * соединённые символом перевода строки, без {@code hash}.
 */
public final class MaxInitDataValidator {

    private static final String HMAC = "HmacSHA256";
    private static final String HASH_KEY = "hash";
    private static final String AUTH_DATE_KEY = "auth_date";
    /** Допустимый рассинхрон часов: auth_date в будущем не дальше этого значения (V-026). */
    private static final long CLOCK_SKEW_SECONDS = 60;

    private final byte[] secretKey;
    private final long maxAgeSeconds;

    public MaxInitDataValidator(String botToken, long maxAgeSeconds) {
        this.secretKey = hmac("WebAppData".getBytes(StandardCharsets.UTF_8),
                botToken.getBytes(StandardCharsets.UTF_8));
        this.maxAgeSeconds = maxAgeSeconds;
    }

    /**
     * Возвращает разобранные параметры (без {@code hash}) или {@link Optional#empty()},
     * если подпись неверна, строка повреждена, {@code hash} отсутствует/дублируется
     * или {@code auth_date} просрочен.
     */
    public Optional<Map<String, String>> validate(String initData) {
        if (initData == null || initData.isBlank()) {
            return Optional.empty();
        }

        Map<String, String> params = new TreeMap<>();
        String hash = null;
        for (String pair : initData.split("&")) {
            int i = pair.indexOf('=');
            if (i <= 0) {
                return Optional.empty();
            }
            String key = pair.substring(0, i);
            String value = URLDecoder.decode(pair.substring(i + 1), StandardCharsets.UTF_8);
            if (HASH_KEY.equals(key)) {
                if (hash != null) {
                    return Optional.empty();
                }
                hash = value;
            } else if (params.put(key, value) != null) {
                return Optional.empty();
            }
        }
        if (hash == null) {
            return Optional.empty();
        }

        String launchParams = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));

        String signature = HexFormat.of().formatHex(hmac(secretKey, launchParams.getBytes(StandardCharsets.UTF_8)));
        if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8),
                hash.toLowerCase().getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }

        if (!isFresh(params.get(AUTH_DATE_KEY))) {
            return Optional.empty();
        }

        return Optional.of(Map.copyOf(params));
    }

    private boolean isFresh(String authDate) {
        try {
            long authDateSeconds = Long.parseLong(authDate == null ? "0" : authDate);
            long age = Instant.now().getEpochSecond() - authDateSeconds;
            return age >= -CLOCK_SKEW_SECONDS && age <= maxAgeSeconds;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(key, HMAC));
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
