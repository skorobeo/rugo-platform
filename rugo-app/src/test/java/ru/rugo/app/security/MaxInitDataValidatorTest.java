package ru.rugo.app.security;

import org.junit.jupiter.api.Test;
import ru.rugo.core.security.MaxInitDataValidator;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тест-векторы сгенерированы независимо скриптом
 * {@code docs/miniapp/tools/gen_initdata_vector.py} (токен {@value #BOT_TOKEN}).
 */
class MaxInitDataValidatorTest {

    private static final String BOT_TOKEN = "test-bot-token-123";
    private static final long FAR_FUTURE_AGE = Long.MAX_VALUE;
    private static final long ONE_MINUTE_AGE = 60L;

    /** basic: auth_date=1758700000, query_id=AAHk9XyZ=, start_param=m_ivan, user=JSON (кириллица). */
    private static final String BASIC =
            "user=%7B%22id%22%3A424242%2C%22first_name%22%3A%22%D0%98%D0%B2%D0%B0%D0%BD%22%2C%22username%22%3A%22ivan_test%22%7D"
                    + "&auth_date=1758700000&query_id=AAHk9XyZ%3D&start_param=m_ivan"
                    + "&hash=5616e2706f4951b9eff3965e58b43d8f3d0a76de5d6d723b2492ff6838ad01cd";

    /** special_and_equals: query_id=a=b&c (спецсимволы '=' и '&'), порядок ключей в строке перемешан. */
    private static final String SPECIAL =
            "query_id=a%3Db%26c&start_param=mgr_01-x"
                    + "&user=%7B%22id%22%3A424242%2C%22first_name%22%3A%22%D0%98%D0%B2%D0%B0%D0%BD%22%2C%22username%22%3A%22ivan_test%22%7D"
                    + "&auth_date=1758700000"
                    + "&hash=f3871b53e43cf617214ed4fac815124d69ae79790c459d4fd40e015d3617d6d6";

    /** non_numeric_auth_date: подпись валидна, но auth_date не число. */
    private static final String NON_NUMERIC_AUTH_DATE =
            "user=%7B%22id%22%3A424242%2C%22first_name%22%3A%22%D0%98%D0%B2%D0%B0%D0%BD%22%2C%22username%22%3A%22ivan_test%22%7D"
                    + "&query_id=AAHk9XyZ%3D&auth_date=not-a-number"
                    + "&hash=b27744bfbebbfcc8f4d4549a144601d079cde5709c95e7cfff69ac8c265de67e";

    private static MaxInitDataValidator validator(long maxAgeSeconds) {
        return new MaxInitDataValidator(BOT_TOKEN, maxAgeSeconds);
    }

    @Test
    void validatesBasicVectorAndDecodesValues() {
        Optional<Map<String, String>> result = validator(FAR_FUTURE_AGE).validate(BASIC);

        assertTrue(result.isPresent());
        Map<String, String> params = result.get();
        assertFalse(params.containsKey("hash"));
        assertEquals("1758700000", params.get("auth_date"));
        assertEquals("AAHk9XyZ=", params.get("query_id"));
        assertEquals("m_ivan", params.get("start_param"));
        assertEquals("{\"id\":424242,\"first_name\":\"Иван\",\"username\":\"ivan_test\"}", params.get("user"));
    }

    @Test
    void validatesVectorWithSpecialCharsAndShuffledKeyOrder() {
        Optional<Map<String, String>> result = validator(FAR_FUTURE_AGE).validate(SPECIAL);

        assertTrue(result.isPresent());
        assertEquals("a=b&c", result.get().get("query_id"));
        assertEquals("mgr_01-x", result.get().get("start_param"));
    }

    @Test
    void acceptsUppercaseHash() {
        String upper = BASIC.replace(
                "5616e2706f4951b9eff3965e58b43d8f3d0a76de5d6d723b2492ff6838ad01cd",
                "5616E2706F4951B9EFF3965E58B43D8F3D0A76DE5D6D723B2492FF6838AD01CD");

        assertTrue(validator(FAR_FUTURE_AGE).validate(upper).isPresent());
    }

    @Test
    void rejectsWrongHash() {
        String tampered = BASIC.replace("ad01cd", "ad01ce");

        assertTrue(validator(FAR_FUTURE_AGE).validate(tampered).isEmpty());
    }

    @Test
    void rejectsTamperedValue() {
        String tampered = BASIC.replace("m_ivan", "m_ivan2");

        assertTrue(validator(FAR_FUTURE_AGE).validate(tampered).isEmpty());
    }

    @Test
    void rejectsDuplicateHash() {
        String duplicated = BASIC + "&hash=5616e2706f4951b9eff3965e58b43d8f3d0a76de5d6d723b2492ff6838ad01cd";

        assertTrue(validator(FAR_FUTURE_AGE).validate(duplicated).isEmpty());
    }

    @Test
    void rejectsMissingHash() {
        String withoutHash = BASIC.substring(0, BASIC.indexOf("&hash="));

        assertTrue(validator(FAR_FUTURE_AGE).validate(withoutHash).isEmpty());
    }

    @Test
    void rejectsDuplicateOtherKey() {
        String duplicated = BASIC + "&start_param=m_ivan";

        assertTrue(validator(FAR_FUTURE_AGE).validate(duplicated).isEmpty());
    }

    @Test
    void rejectsExpiredAuthDate() {
        assertTrue(validator(ONE_MINUTE_AGE).validate(BASIC).isEmpty());
    }

    @Test
    void rejectsMalformedStrings() {
        assertTrue(validator(FAR_FUTURE_AGE).validate(null).isEmpty());
        assertTrue(validator(FAR_FUTURE_AGE).validate("").isEmpty());
        assertTrue(validator(FAR_FUTURE_AGE).validate("   ").isEmpty());
        assertTrue(validator(FAR_FUTURE_AGE).validate("no-equals-sign").isEmpty());
        assertTrue(validator(FAR_FUTURE_AGE).validate("=novaluekey&hash=abc").isEmpty());
        assertTrue(validator(FAR_FUTURE_AGE).validate("a=1&hash=ZZ").isEmpty());
    }

    @Test
    void rejectsNonNumericAuthDateEvenWhenSignatureValid() {
        assertTrue(validator(FAR_FUTURE_AGE).validate(NON_NUMERIC_AUTH_DATE).isEmpty());
    }

    @Test
    void rejectsAuthDateFarInFuture() {
        long future = Instant.now().getEpochSecond() + 3600;
        assertTrue(validator(FAR_FUTURE_AGE).validate(signedAt(future)).isEmpty());
    }

    @Test
    void acceptsAuthDateWithinClockSkew() {
        long nearFuture = Instant.now().getEpochSecond() + 30;
        assertTrue(validator(ONE_MINUTE_AGE).validate(signedAt(nearFuture)).isPresent());
    }

    /** Строит корректно подписанный initData с заданным auth_date (тем же алгоритмом). */
    private static String signedAt(long authDateSeconds) {
        Map<String, String> params = new TreeMap<>();
        params.put("auth_date", Long.toString(authDateSeconds));
        params.put("user", "{\"id\":424242}");
        String launch = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("\n"));
        byte[] secret = hmac("WebAppData".getBytes(StandardCharsets.UTF_8), BOT_TOKEN.getBytes(StandardCharsets.UTF_8));
        String hash = HexFormat.of().formatHex(hmac(secret, launch.getBytes(StandardCharsets.UTF_8)));
        return "user=%7B%22id%22%3A424242%7D&auth_date=" + authDateSeconds + "&hash=" + hash;
    }

    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
