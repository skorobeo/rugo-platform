package ru.rugo.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextRulesTest {

    @Test
    void plainAllowsSafeTextAndRejectsInjection() {
        assertTrue(TextRules.PLAIN.matcher("Экскаватор JCB 3CX").matches());
        assertTrue(TextRules.PLAIN.matcher("«Марка» (нов.) №1").matches());
        assertFalse(TextRules.PLAIN.matcher("<script>alert(1)</script>").matches());
        assertFalse(TextRules.PLAIN.matcher("a & b").matches());
        assertFalse(TextRules.PLAIN.matcher("quote \" x").matches());
    }

    @Test
    void descriptionAllowsColonPercentAndNewlines() {
        assertTrue(TextRules.DESCRIPTION.matcher("Ремонт: 100% качество\nсрочно").matches());
        assertFalse(TextRules.DESCRIPTION.matcher("<b>bold</b>").matches());
    }

    @Test
    void imageUrlAcceptsLocalPathAndHttps() {
        assertTrue(TextRules.IMAGE_URL.matcher("/uploads/equipment/ekskavator.png").matches());
        assertTrue(TextRules.IMAGE_URL.matcher("https://cdn.example.com/a/b.jpg").matches());
        assertFalse(TextRules.IMAGE_URL.matcher("/etc/passwd").matches());
        assertFalse(TextRules.IMAGE_URL.matcher("javascript:alert(1)").matches());
        assertFalse(TextRules.IMAGE_URL.matcher("https://x.com/a b.jpg").matches());
    }
}
