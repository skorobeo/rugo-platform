package ru.rugo.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SourceCodeTest {

    @Test
    void keepsAllowedCharacters() {
        assertEquals("mgr_01-x", SourceCode.sanitize("mgr_01-x"));
    }

    @Test
    void stripsDisallowedCharacters() {
        assertEquals("mgr01", SourceCode.sanitize("mgr;01"));
        assertEquals("abc", SourceCode.sanitize(" a b c "));
    }

    @Test
    void returnsNullForBlankOrNull() {
        assertNull(SourceCode.sanitize(null));
        assertNull(SourceCode.sanitize("   "));
        assertNull(SourceCode.sanitize("!!!"));
    }

    @Test
    void truncatesTo512() {
        String value = "a".repeat(600);
        assertEquals(512, SourceCode.sanitize(value).length());
    }
}
