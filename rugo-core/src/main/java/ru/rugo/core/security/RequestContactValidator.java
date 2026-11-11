package ru.rugo.core.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Проверка подписи ответа {@code WebApp.requestContact()} (мини-приложение MAX).
 *
 * <p>Документация (dev.max.ru/docs/webapps/bridge → «Запрос номера телефона»):
 * подпись — это {@code hex(HMAC_SHA256(botToken, launch_params))}, где
 * {@code launch_params} — пары {@code key=value} в алфавитном порядке,
 * соединённые символом {@code \n}: {@code authDate}, {@code phone}, {@code userId}.
 * {@code phone} должен быть без {@code +}. Проверка подтверждает, что пользователь
 * поделился номером, привязанным к его аккаунту в MAX.
 *
 * <p>Если токен бота не задан (dev/test), проверка невозможна: {@link #isEnabled()}
 * возвращает {@code false}, и вызывающий код не блокирует заявку.
 */
public final class RequestContactValidator {

    private static final String HMAC = "HmacSHA256";

    private final String botToken;

    public RequestContactValidator(String botToken) {
        this.botToken = botToken == null ? "" : botToken;
    }

    /** Проверка имеет смысл только при заданном токене бота. */
    public boolean isEnabled() {
        return !botToken.isBlank();
    }

    /**
     * Сверяет {@code hash} из ответа {@code requestContact} с локально вычисленной
     * подписью. Все входные данные обязательны; при любом пропуске — {@code false}.
     */
    public boolean isValid(String phone, String authDate, String hash, Long userId) {
        if (!isEnabled() || phone == null || authDate == null || authDate.isBlank()
                || hash == null || hash.isBlank() || userId == null) {
            return false;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return false;
        }
        String launchParams = "authDate=" + authDate + "\n"
                + "phone=" + digits + "\n"
                + "userId=" + userId;

        String expected = HexFormat.of().formatHex(hmac(botToken.getBytes(StandardCharsets.UTF_8),
                launchParams.getBytes(StandardCharsets.UTF_8)));
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                hash.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * То же, что {@link #isValid(String, String, String, Long)}, плюс проверка свежести
     * {@code authDate}: подпись старше {@code maxAgeSeconds} (или из будущего) не принимается.
     * Защищает от повторного использования перехваченного ответа {@code requestContact}.
     * {@code maxAgeSeconds <= 0} — проверка свежести выключена.
     */
    public boolean isValid(String phone, String authDate, String hash, Long userId, long maxAgeSeconds) {
        if (!isValid(phone, authDate, hash, userId)) {
            return false;
        }
        if (maxAgeSeconds <= 0) {
            return true;
        }
        try {
            long auth = Long.parseLong(authDate.trim());
            long now = Instant.now().getEpochSecond();
            long age = now - auth;
            // допускаем небольшой рассинхрон часов (до 5 минут «в будущее»)
            return age >= -300 && age <= maxAgeSeconds;
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
