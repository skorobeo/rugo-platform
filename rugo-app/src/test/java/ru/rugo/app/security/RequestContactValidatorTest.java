package ru.rugo.app.security;

import org.junit.jupiter.api.Test;
import ru.rugo.core.security.RequestContactValidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тест-вектор сгенерирован независимо (Python):
 * {@code hmac.new(b"123456:TESTTOKEN", b"authDate=1771409719\nphone=79001234567\nuserId=67890", sha256).hexdigest()}.
 */
class RequestContactValidatorTest {

    private static final String BOT_TOKEN = "123456:TESTTOKEN";
    private static final String AUTH_DATE = "1771409719";
    private static final Long USER_ID = 67890L;
    private static final String VALID_HASH =
            "5aab6c8d133aa5e7072b67cc0615d9af7c8c492edeb71e3a93f8b131266c252e";

    private final RequestContactValidator validator = new RequestContactValidator(BOT_TOKEN);

    @Test
    void acceptsValidSignatureForPhoneWithPlus() {
        assertTrue(validator.isValid("+79001234567", AUTH_DATE, VALID_HASH, USER_ID));
    }

    @Test
    void acceptsValidSignatureCaseInsensitiveHash() {
        assertTrue(validator.isValid("79001234567", AUTH_DATE, VALID_HASH.toUpperCase(), USER_ID));
    }

    @Test
    void rejectsWrongPhone() {
        assertFalse(validator.isValid("+79001234568", AUTH_DATE, VALID_HASH, USER_ID));
    }

    @Test
    void rejectsWrongUserId() {
        assertFalse(validator.isValid("+79001234567", AUTH_DATE, VALID_HASH, 1L));
    }

    @Test
    void rejectsMissingData() {
        assertFalse(validator.isValid(null, AUTH_DATE, VALID_HASH, USER_ID));
        assertFalse(validator.isValid("+79001234567", null, VALID_HASH, USER_ID));
        assertFalse(validator.isValid("+79001234567", AUTH_DATE, null, USER_ID));
        assertFalse(validator.isValid("+79001234567", AUTH_DATE, VALID_HASH, null));
        assertFalse(validator.isValid("", AUTH_DATE, VALID_HASH, USER_ID));
    }

    @Test
    void disabledWhenTokenBlank() {
        RequestContactValidator noToken = new RequestContactValidator("");
        assertFalse(noToken.isEnabled());
        assertFalse(noToken.isValid("+79001234567", AUTH_DATE, VALID_HASH, USER_ID));
    }
}
