package ru.rugo.app.service;

import org.junit.jupiter.api.Test;
import ru.rugo.core.util.AnalyticsHash;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyticsHashTest {

    /** Регрессионный вектор: HMAC_SHA256(key="salt", data="u:42"). Менять формат нельзя. */
    @Test
    void userHashMatchesKnownVector() {
        assertEquals("49c48d8d47aa77b23aa211bc88dadcf39f1b75c8cbe5b339c6bb223be9b9a529",
                AnalyticsHash.userHash("salt", 42L));
    }

    @Test
    void adminOwnerHashIsStable() {
        assertEquals("7a16d2e55c4202bcbaa9fc70bb746b5dd582ca794d14ee53fe53aae5f947d3a4",
                AnalyticsHash.userHash("salt", 185547050L));
    }

    @Test
    void nullSaltEqualsEmptySalt() {
        assertEquals(AnalyticsHash.userHash("", 1L), AnalyticsHash.userHash(null, 1L));
    }
}
