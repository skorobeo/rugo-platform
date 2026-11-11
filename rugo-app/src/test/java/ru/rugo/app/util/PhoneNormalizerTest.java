package ru.rugo.app.util;

import org.junit.jupiter.api.Test;
import ru.rugo.core.util.PhoneNormalizer;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhoneNormalizerTest {

    @Test
    void normalizesVariousFormatsToE164() {
        assertEquals(Optional.of("+79001234567"), PhoneNormalizer.normalize("+7 (900) 123-45-67"));
        assertEquals(Optional.of("+79001234567"), PhoneNormalizer.normalize("8 900 123 45 67"));
        assertEquals(Optional.of("+79001234567"), PhoneNormalizer.normalize("9001234567"));
        assertEquals(Optional.of("+79001234567"), PhoneNormalizer.normalize("79001234567"));
        assertEquals(Optional.of("+79001234567"), PhoneNormalizer.normalize("8(900)1234567"));
    }

    @Test
    void rejectsInvalidInput() {
        assertTrue(PhoneNormalizer.normalize(null).isEmpty());
        assertTrue(PhoneNormalizer.normalize("").isEmpty());
        assertTrue(PhoneNormalizer.normalize("123").isEmpty());
        assertTrue(PhoneNormalizer.normalize("abc").isEmpty());
        assertTrue(PhoneNormalizer.normalize("+1 202 555 0143").isEmpty());
        assertTrue(PhoneNormalizer.normalize("900123456789").isEmpty());
    }
}
